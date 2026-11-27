import 'dart:async';

import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/user.dart';
import '../session/account.dart';
import '../push/update.dart';
import '../utils/logger.dart';
import 'login.dart';
import 'widget/avatar.dart';

class AccountsPage extends StatefulWidget {
  const AccountsPage({super.key});
  @override
  State<AccountsPage> createState() => _AccountsPageState();
}

class _AccountsPageState extends State<AccountsPage> {
  List<User> _accounts = [];
  final Set<String> _selected = <String>{};
  bool _multiSelect = false;
  String? _currentId;
  StreamSubscription? _subscription;

  @override
  void initState() {
    super.initState();
    _load();
    _subscription = AccountChangeNotifier().accountChanges.listen((_) {
      if (mounted) _load();
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  void _load() {
    setState(() {
      _accounts = AccountManager.allAccounts;
      _currentId = AccountManager.currentSessionId;
    });
    final invalid = _accounts.where((u) => !u.status).length;
    if (invalid > 0 && mounted) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text('有 $invalid 个账号已失效，请重新登录')));
        }
      });
    }
  }

  Future<void> _openLogin(String type) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => LoginPage(initialLoginType: type)),
    );
    if (result == true) _load();
  }

  Future<void> _showQRCodeLogin() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const LoginPage(initialLoginType: 'qrcode')),
    );
    if (result == true) _load();
  }

  void _showAbout() async {
    final info = await PackageInfo.fromPlatform();
    if (!mounted) return;
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('关于'),
        content: SingleChildScrollView(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('长江雨课堂助手', style: TextStyle(fontSize: 17)),
            const SizedBox(height: 8),
            Text('版本 ${info.version}'),
            const SizedBox(height: 8),
            const Text('仅服务长江雨课堂（changjiang.yuketang.cn）的独立客户端。'),
            const SizedBox(height: 8),
            const Text('基于 course_helper 裁剪重构，遵循 GPL v3 协议。'),
          ]),
        ),
        actions: [
          TextButton(
            onPressed: () async {
              final uri = Uri.parse('https://github.com/AneryCoft/course_helper');
              if (await canLaunchUrl(uri)) {
                await launchUrl(uri, mode: LaunchMode.externalApplication);
              }
            },
            child: const Text('原项目'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('账号'),
        actions: [
          if (_multiSelect)
            IconButton(
              icon: const Icon(Icons.delete),
              onPressed: () async {
                await AccountManager.removeAccounts(_selected.toList());
                _selected.clear();
                setState(() => _multiSelect = false);
              },
            ),
          IconButton(
            icon: Icon(_multiSelect ? Icons.done : Icons.select_all),
            onPressed: () => setState(() {
              _multiSelect = !_multiSelect;
              if (!_multiSelect) _selected.clear();
            }),
          ),
          PopupMenuButton<String>(
            onSelected: (v) {
              if (v == 'about') _showAbout();
              if (v == 'update') Update.checkUpdate(ignoreSkip: true);
              if (v == 'log') exportLog();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: 'update', child: Text('检查更新')),
              PopupMenuItem(value: 'log', child: Text('导出日志')),
              PopupMenuItem(value: 'about', child: Text('关于')),
            ],
          ),
        ],
      ),
      body: _accounts.isEmpty
          ? const Center(
              child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                Icon(Icons.account_circle_outlined, size: 64, color: Colors.grey),
                SizedBox(height: 12),
                Text('暂无账号，点击右下角添加', style: TextStyle(color: Colors.grey)),
              ]),
            )
          : ReorderableListView.builder(
              itemCount: _accounts.length,
              onReorder: (oldIndex, newIndex) async {
                if (newIndex > oldIndex) newIndex--;
                await AccountManager.moveAccount(oldIndex, newIndex);
              },
              itemBuilder: (context, index) {
                final user = _accounts[index];
                return Card(
                  key: ValueKey(user.uid),
                  margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: ListTile(
                    leading: AvatarWidget(imageUrl: user.avatar, size: 46),
                    title: Row(children: [
                      Text(user.name),
                      if (user.uid == _currentId)
                        Container(
                          margin: const EdgeInsets.only(left: 8),
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                          decoration: BoxDecoration(
                            color: Theme.of(context).colorScheme.primary,
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Text('当前',
                              style: TextStyle(color: Colors.white, fontSize: 10)),
                        ),
                    ]),
                    subtitle: Text('ID: ${user.uid}\n${user.school}'),
                    trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                      if (!user.status)
                        Tooltip(
                          message: '账号失效，请重新登录',
                          child: Icon(Icons.error_outline, color: Colors.red, size: 28),
                        ),
                      if (_multiSelect)
                        Checkbox(
                          value: _selected.contains(user.uid),
                          onChanged: (v) => setState(() {
                            if (v == true) {
                              _selected.add(user.uid);
                            } else {
                              _selected.remove(user.uid);
                            }
                          }),
                        ),
                    ]),
                    onTap: _multiSelect
                        ? null
                        : () async {
                            await AccountManager.setCurrentSession(user.uid);
                          },
                  ),
                );
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => showModalBottomSheet(
          context: context,
          builder: (context) => SafeArea(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              ListTile(
                leading: const Icon(Icons.qr_code),
                title: const Text('二维码登录'),
                onTap: () {
                  Navigator.pop(context);
                  _showQRCodeLogin();
                },
              ),
              ListTile(
                leading: const Icon(Icons.sms),
                title: const Text('验证码登录'),
                onTap: () {
                  Navigator.pop(context);
                  _openLogin('captcha');
                },
              ),
              ListTile(
                leading: const Icon(Icons.password),
                title: const Text('密码登录'),
                onTap: () {
                  Navigator.pop(context);
                  _openLogin('password');
                },
              ),
            ]),
          ),
        ),
        child: const Icon(Icons.add),
      ),
    );
  }
}
