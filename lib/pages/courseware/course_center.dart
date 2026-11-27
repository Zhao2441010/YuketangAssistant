import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_cache_manager/flutter_cache_manager.dart';
import 'package:photo_manager/photo_manager.dart';

import '../../api/content.dart';
import '../../api/api_service.dart';
import '../../models/course.dart';
import '../../models/courseware.dart';

/// 课程中心：全部课程 → 历史课时 → 课件 PPT + 顶部测验区
class CourseCenterPage extends StatefulWidget {
  const CourseCenterPage({super.key});
  @override
  State<CourseCenterPage> createState() => _CourseCenterPageState();
}

class _CourseCenterPageState extends State<CourseCenterPage> {
  String _view = 'courses';
  List<dynamic> _courses = [];
  List<CoursewareActivity> _activities = [];
  List<CoursewarePresentation> _presentations = [];
  Course? _selectedCourse;
  CoursewareActivity? _selectedLesson;
  bool _loading = false;
  String? _error;
  int _presIndex = 0;
  int _slideIndex = 0;
  bool _showProblems = true;
  bool _downloading = false;
  int _downloaded = 0;
  int _downloadTotal = 0;
  final PageController _pageController = PageController();
  final ScrollController _thumbController = ScrollController();

  @override
  void initState() {
    super.initState();
    _loadCourses();
  }

  Future<void> _loadCourses() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    final data = await RCContentApi.getLearningList();
    if (!mounted) return;
    setState(() {
      _courses = data ?? [];
      _loading = false;
    });
  }

  Future<void> _enterCourse(dynamic raw) async {
    final courseId = (raw['course_id'] ?? raw['courseId'] ?? '').toString();
    final name = (raw['name'] ?? raw['course_name'] ?? '课程').toString();
    final teacher = (raw['teacher_name'] ?? raw['teacher'] ?? '').toString();
    setState(() {
      _selectedCourse = Course(
        courseId: courseId,
        classId: courseId,
        name: name,
        teacher: teacher,
        image: (raw['image'] ?? raw['cover'] ?? '').toString(),
        state: true,
      );
      _view = 'activities';
      _loading = true;
      _error = null;
    });
    final data = await RCContentApi.getActivities(courseId);
    if (!mounted) return;
    setState(() {
      _activities = (data ?? [])
          .whereType<Map>()
          .map((e) => CoursewareActivity.fromJson(Map<String, dynamic>.from(e)))
          .toList();
      _loading = false;
    });
  }

  Future<void> _enterLesson(CoursewareActivity activity) async {
    final lessonId = activity.coursewareId.isNotEmpty
        ? activity.coursewareId
        : activity.activityId;
    setState(() {
      _selectedLesson = activity;
      _view = 'slides';
      _loading = true;
      _error = null;
      _presIndex = 0;
      _slideIndex = 0;
      _showProblems = true;
    });
    final rawPres = await RCContentApi.fetchPresentations(lessonId);
    if (rawPres == null || rawPres.isEmpty) {
      if (mounted) setState(() => _loading = false);
      return;
    }
    final pres = <CoursewarePresentation>[];
    for (final raw in rawPres.whereType<Map>()) {
      final map = Map<String, dynamic>.from(raw);
      final base = CoursewarePresentation.fromJson(map);
      final pid = base.id;
      final rawSlides = await RCContentApi.fetchSlides(pid, lessonId);
      final slides = (rawSlides ?? [])
          .whereType<Map>()
          .map((e) => CoursewareSlide.fromJson(Map<String, dynamic>.from(e)))
          .toList();
      pres.add(base.withSlides(slides));
    }
    if (!mounted) return;
    setState(() {
      _presentations = pres;
      _loading = false;
    });
  }

  List<Map<String, dynamic>> get _quizItems {
    final items = <Map<String, dynamic>>[];
    for (int pi = 0; pi < _presentations.length; pi++) {
      final p = _presentations[pi];
      for (int si = 0; si < p.slides.length; si++) {
        final s = p.slides[si];
        if (s.problem != null) {
          items.add({
            'presIdx': pi,
            'slideIdx': si,
            'page': s.page,
            'presTitle': p.title,
            'problem': s.problem,
          });
        }
      }
    }
    return items;
  }

  void _jumpTo(int presIdx, int slideIdx) {
    setState(() {
      _presIndex = presIdx;
      _slideIndex = slideIdx;
    });
    if (_pageController.hasClients) {
      _pageController.animateToPage(slideIdx,
          duration: const Duration(milliseconds: 300), curve: Curves.easeInOut);
    }
  }

  Future<void> _downloadAll() async {
    if (_downloading || _presentations.isEmpty) return;
    final urls = _presentations[_presIndex].slides
        .map((s) => s.imageUrl)
        .where((u) => u.isNotEmpty)
        .toList();
    if (urls.isEmpty) return;
    setState(() {
      _downloading = true;
      _downloaded = 0;
      _downloadTotal = urls.length;
    });
    int success = 0;
    final title = _presentations[_presIndex].title.isNotEmpty
        ? _presentations[_presIndex].title
        : (_selectedLesson?.title ?? '课件');
    for (int i = 0; i < urls.length; i++) {
      try {
        final file = await DefaultCacheManager().getSingleFile(urls[i]);
        await PhotoManager.editor.saveImage(
          await file.readAsBytes(),
          filename: '${i + 1}.jpg',
          title: '${i + 1}',
          relativePath: 'Pictures/$title',
        );
        success++;
      } catch (_) {}
      if (mounted) setState(() => _downloaded = i + 1);
    }
    if (mounted) {
      setState(() => _downloading = false);
      ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('已保存 $success/${urls.length} 页')));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_view == 'courses') return _buildCourses();
    if (_view == 'activities') return _buildActivities();
    return _buildSlides();
  }

  Widget _header(String title, String subtitle, VoidCallback onBack) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(children: [
          IconButton(onPressed: onBack, icon: const Icon(Icons.arrow_back)),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title,
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600)),
                Text(subtitle,
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
              ],
            ),
          ),
        ]),
        const SizedBox(height: 8),
      ],
    );
  }

  Widget _buildCourses() {
    return Scaffold(
      appBar: AppBar(title: const Text('课程中心')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadCourses,
              child: ListView.builder(
                padding: const EdgeInsets.all(12),
                itemCount: _courses.length,
                itemBuilder: (context, index) {
                  final raw = _courses[index];
                  final name = (raw['name'] ?? raw['course_name'] ?? '课程').toString();
                  final teacher =
                      (raw['teacher_name'] ?? raw['teacher'] ?? '').toString();
                  final status = raw['status']?.toString();
                  return Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20)),
                    child: ListTile(
                      contentPadding: const EdgeInsets.all(16),
                      leading: Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: Colors.blue.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const Icon(Icons.menu_book, color: Colors.blue),
                      ),
                      title: Text(name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text(teacher,
                          maxLines: 1, overflow: TextOverflow.ellipsis),
                      trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                        if (status == '1')
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: Colors.green.shade50,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text('上课中',
                                style: TextStyle(
                                    fontSize: 10, color: Colors.green.shade700)),
                          ),
                        const Icon(Icons.chevron_right, color: Colors.grey),
                      ]),
                      onTap: () => _enterCourse(raw),
                    ),
                  );
                },
              ),
            ),
    );
  }

  Widget _buildActivities() {
    return Scaffold(
      appBar: AppBar(title: Text(_selectedCourse?.name ?? '课程')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(12),
              children: [
                _header('选择课时', '查看该课时的历史课件与题目',
                    () => setState(() => _view = 'courses')),
                if (_error != null) Text(_error!, style: const TextStyle(color: Colors.red)),
                if (_activities.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(32),
                    child: Center(child: Text('暂无课时记录')),
                  ),
                ..._activities.map((a) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
                    child: ListTile(
                      leading: Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: a.type == 4
                              ? Colors.orange.shade50
                              : Colors.purple.shade50,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Icon(
                          a.type == 4 ? Icons.assignment : Icons.slideshow,
                          color: a.type == 4 ? Colors.orange : Colors.purple,
                        ),
                      ),
                      title: Text(a.title.isEmpty ? a.typeName : a.title),
                      subtitle: Text(
                        '${a.typeName} · ${_formatTime(a.createTime)}',
                        style: const TextStyle(fontSize: 11),
                      ),
                      trailing: a.finished
                          ? const Icon(Icons.check_circle, color: Colors.green)
                          : const Icon(Icons.chevron_right, color: Colors.grey),
                      onTap: () => _enterLesson(a),
                    ),
                  );
                }),
              ],
            ),
    );
  }

  String _formatTime(int ms) {
    if (ms <= 0) return '';
    final dt = DateTime.fromMillisecondsSinceEpoch(
        ms.toString().length <= 10 ? ms * 1000 : ms);
    return '${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  Widget _buildSlides() {
    final pres = _presentations.isEmpty ? null : _presentations[_presIndex];
    final quizzes = _quizItems;
    return Scaffold(
      appBar: AppBar(
        title: Text(_selectedLesson?.title ?? '历史课件'),
        actions: [
          IconButton(
            onPressed: _downloadAll,
            tooltip: '下载全部页',
            icon: _downloading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.download),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : pres == null
              ? const Center(child: Text('该课时暂无课件'))
              : Column(
                  children: [
                    // 课件与测验解耦：教师题目置顶集中展示
                    if (quizzes.isNotEmpty)
                      Card(
                        margin: const EdgeInsets.fromLTRB(12, 8, 12, 4),
                        color: Colors.amber.shade50,
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16)),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            InkWell(
                              onTap: () =>
                                  setState(() => _showProblems = !_showProblems),
                              child: Padding(
                                padding: const EdgeInsets.all(12),
                                child: Row(children: [
                                  const Icon(Icons.quiz, color: Colors.amber),
                                  const SizedBox(width: 8),
                                  Text('教师题目 · 测验区（${quizzes.length}）',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.w600)),
                                  const Spacer(),
                                  Icon(_showProblems
                                      ? Icons.expand_less
                                      : Icons.expand_more),
                                ]),
                              ),
                            ),
                            if (_showProblems)
                              Padding(
                                padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                                child: Column(
                                  children: quizzes.map((q) {
                                    return _QuizItem(
                                      data: q,
                                      onTap: () => _jumpTo(
                                          q['presIdx'] as int,
                                          q['slideIdx'] as int),
                                    );
                                  }).toList(),
                                ),
                              ),
                          ],
                        ),
                      ),
                    // 课件切换栏
                    if (_presentations.length > 1)
                      SizedBox(
                        height: 44,
                        child: ListView.builder(
                          scrollDirection: Axis.horizontal,
                          padding: const EdgeInsets.symmetric(horizontal: 12),
                          itemCount: _presentations.length,
                          itemBuilder: (context, i) => Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: ChoiceChip(
                              label: Text(_presentations[i].title.isEmpty
                                  ? '课件 ${i + 1}'
                                  : _presentations[i].title),
                              selected: i == _presIndex,
                              onSelected: (_) {
                                setState(() {
                                  _presIndex = i;
                                  _slideIndex = 0;
                                });
                                _pageController.jumpToPage(0);
                              },
                            ),
                          ),
                        ),
                      ),
                    // PPT 主图
                    Expanded(
                      child: PageView.builder(
                        controller: _pageController,
                        itemCount: pres.slides.length,
                        onPageChanged: (i) {
                          setState(() => _slideIndex = i);
                          if (_thumbController.hasClients) {
                            _thumbController.animateTo(
                              (i * 76).toDouble(),
                              duration: const Duration(milliseconds: 200),
                              curve: Curves.easeOut,
                            );
                          }
                        },
                        itemBuilder: (context, i) {
                          final slide = pres.slides[i];
                          return slide.imageUrl.isEmpty
                              ? const Center(child: Text('暂无图片'))
                              : InteractiveViewer(
                                  minScale: 1,
                                  maxScale: 4,
                                  child: Image.network(slide.imageUrl,
                                      fit: BoxFit.contain),
                                );
                        },
                      ),
                    ),
                    // 页码 + 缩略图条
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 4),
                      child: Text(
                        '${pres.slides.isEmpty ? 0 : pres.slides[_slideIndex].page} / '
                        '${pres.slides.length}'
                        '${_downloading ? '  已保存 $_downloaded/$_downloadTotal' : ''}',
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                    SizedBox(
                      height: 76,
                      child: ListView.builder(
                        controller: _thumbController,
                        scrollDirection: Axis.horizontal,
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        itemCount: pres.slides.length,
                        itemBuilder: (context, i) {
                          final slide = pres.slides[i];
                          return GestureDetector(
                            onTap: () => _jumpTo(_presIndex, i),
                            child: Container(
                              width: 68,
                              margin: const EdgeInsets.only(right: 8),
                              padding: const EdgeInsets.all(2),
                              decoration: BoxDecoration(
                                border: Border.all(
                                  color: i == _slideIndex
                                      ? Theme.of(context).colorScheme.primary
                                      : Colors.transparent,
                                  width: 2,
                                ),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: slide.imageUrl.isEmpty
                                  ? const Icon(Icons.image)
                                  : Image.network(slide.imageUrl,
                                      fit: BoxFit.cover),
                            ),
                          );
                        },
                      ),
                    ),
                  ],
                ),
    );
  }
}

/// 测验区条目：解析 problem 正文、选项与参考答案，并高亮正确选项
class _QuizItem extends StatelessWidget {
  final Map<String, dynamic> data;
  final VoidCallback onTap;
  const _QuizItem({required this.data, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final Map<String, dynamic>? problem = _parseProblem(data['problem'] as String?);
    final body = problem?['body']?.toString() ?? '教师题目';
    final options = problem?['options'];
    final answer = problem?['answer'];
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        onTap: onTap,
        title: Text(body, maxLines: 3, overflow: TextOverflow.ellipsis),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('课件 ${data['presTitle']} · 第 ${data['page']} 页',
                style: const TextStyle(fontSize: 11)),
            if (options is List)
              ...options.take(6).map((o) {
                final map = o is Map ? o : <String, dynamic>{};
                final key = (map['key'] ?? '').toString();
                final value = (map['value'] ?? o.toString()).toString();
                final isCorrect = answer != null &&
                    (answer.toString().contains(key) ||
                        answer.toString().contains(value));
                return Text(
                  '$key. $value${isCorrect ? '  ✓ 参考答案' : ''}',
                  style: TextStyle(
                    fontSize: 12,
                    color: isCorrect ? Colors.green.shade700 : null,
                    fontWeight: isCorrect ? FontWeight.w600 : null,
                  ),
                );
              }),
            if (answer != null)
              Text('答案：${answer}',
                  style: TextStyle(fontSize: 12, color: Colors.green.shade700)),
          ],
        ),
        trailing: const Icon(Icons.open_in_new, size: 16),
      ),
    );
  }

  Map<String, dynamic>? _parseProblem(String? raw) {
    if (raw == null || raw.isEmpty) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) {
        final content = decoded['content'];
        if (content is Map) {
          return {
            ...content,
            'problemType': content['problemType'] ?? decoded['problemType'],
          };
        }
        return Map<String, dynamic>.from(decoded);
      }
    } catch (_) {}
    return {'body': raw};
  }
}
