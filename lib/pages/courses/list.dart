import 'dart:async';

import 'package:flutter/material.dart';

import '../../api/api_service.dart';
import '../../api/course.dart';
import '../../models/course.dart';
import '../../session/account.dart';
import '../../utils/logger.dart';
import '../widget/avatar.dart';
import '../widget/scan.dart';
import '../presentation/presentation.dart';
import '../courseware/course_center.dart';

class CoursesPage extends StatefulWidget {
  const CoursesPage({super.key});
  @override
  CoursesPageState createState() => CoursesPageState();
}

class CoursesPageState extends State<CoursesPage> {
  List<Course> _courses = [];
  bool _loading = true;
  bool _visible = true;
  Timer? _refreshTimer;
  List<dynamic> _lastOnLesson = [];
  bool _hasInitial = false;
  StreamSubscription? _accountSub;

  @override
  void initState() {
    super.initState();
    _loadCourses();
    _accountSub = AccountChangeNotifier().accountChanges.listen((_) {
      _lastOnLesson = [];
      _hasInitial = false;
      _loadCourses();
    });
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _accountSub?.cancel();
    super.dispose();
  }

  void onVisibilityChanged(bool visible) {
    _visible = visible;
    if (visible) {
      _startPolling();
    } else {
      _refreshTimer?.cancel();
    }
  }

  void _startPolling() {
    if (!_visible || _refreshTimer != null && _refreshTimer!.isActive) return;
    _refreshTimer = Timer.periodic(const Duration(seconds: 3), (_) async {
      if (!mounted || !AccountManager.hasActiveSession()) return;
      try {
        final onLesson = await RCCourseApi.getOnLesson();
        if (onLesson != null && mounted) {
          if (_lastOnLesson.toString() != onLesson.toString()) {
            _lastOnLesson = onLesson;
            _hasInitial = true;
            await _loadCourses(onLesson);
          }
        }
      } catch (e) {
        logger.e('轮询课堂失败：$e');
      }
    });
  }

  Future<void> _loadCourses([List<dynamic>? onLesson]) async {
    if (!AccountManager.hasActiveSession()) {
      if (mounted) setState(() => _courses = []);
      return;
    }
    try {
      final data = await RCCourseApi.getCoursesList(onLesson);
      if (mounted) {
        setState(() {
          _courses = data ?? [];
          _loading = false;
        });
      }
    } catch (e) {
      logger.e('加载课程失败：$e');
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _handleScan(String result) async {
    if (!result.startsWith('http')) return;
    final uri = Uri.parse(result);
    final baseUrl = uri.origin + uri.path;
    if (!baseUrl.contains('/api/v3/lesson/check-in/dynamic-qr-code')) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('不是长江雨课堂动态码')));
      }
      return;
    }
    if (!AccountManager.hasActiveSession()) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('请先登录账号')));
      }
      return;
    }
    final users = AccountManager.allAccounts;
    int success = 0;
    for (final user in users) {
      final code = await RCCourseApi(user).scan(result);
      if (code == 0) success++;
    }
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('扫码签到完成：$success/${users.length}')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('长江雨课堂助手'),
        actions: [
          IconButton(
            icon: const Icon(Icons.qr_code_scanner),
            tooltip: '扫描动态二维码',
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => ScanPage(onScanResult: _handleScan),
              ),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => _loadCourses(),
        child: ListView(
          padding: const EdgeInsets.all(12),
          children: [
            // ========== 顶部：所有课程入口 + 课间测验入口 ==========
            Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20)),
              child: ListTile(
                contentPadding: const EdgeInsets.all(16),
                leading: Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: Colors.blue.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(Icons.grid_view, color: Colors.blue),
                ),
                title: const Text('所有课程',
                    style: TextStyle(fontWeight: FontWeight.w600)),
                subtitle: const Text('课程列表 → 具体课程 → 历史课件'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const CourseCenterPage()),
                ),
              ),
            ),
            Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20)),
              child: ListTile(
                contentPadding: const EdgeInsets.all(16),
                leading: Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: Colors.amber.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Icon(Icons.quiz, color: Colors.amber),
                ),
                title: const Text('课间 · 测验区',
                    style: TextStyle(fontWeight: FontWeight.w600)),
                subtitle: const Text('课件内嵌题目置顶，跳页查看参考答案'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const CourseCenterPage()),
                ),
              ),
            ),
            const SizedBox(height: 12),
            // ========== 下方：当前进行中课程 ==========
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: Row(children: [
                const Icon(Icons.sensors, size: 18, color: Colors.red),
                const SizedBox(width: 6),
                const Text('当前进行中课程',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                const Spacer(),
                TextButton(onPressed: () => _loadCourses(), child: const Text('刷新')),
              ]),
            ),
            if (_loading)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_courses.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(
                  child: Text('暂无正在上课的课程',
                      style: TextStyle(color: Colors.grey)),
                ),
              )
            else
              ..._courses.map((course) => Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20)),
                    child: ListTile(
                      contentPadding: const EdgeInsets.all(14),
                      leading: AvatarWidget(imageUrl: course.image, size: 48),
                      title: Text(course.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text(course.teacher),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => PresentationPage(
                            lessonId: course.lessonId!,
                            title: course.name,
                          ),
                        ),
                      ),
                    ),
                  )),
          ],
        ),
      ),
    );
  }
}
