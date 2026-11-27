/// 历史课件领域模型（与参考项目 CourseCenter.tsx 的前端格式对齐）

class CoursewareActivity {
  final String activityId;
  final String coursewareId;
  final String title;
  final int type;
  final String typeName;
  final int createTime;
  final bool finished;

  CoursewareActivity({
    required this.activityId,
    required this.coursewareId,
    required this.title,
    required this.type,
    required this.typeName,
    required this.createTime,
    required this.finished,
  });

  factory CoursewareActivity.fromJson(Map<String, dynamic> json) {
    final int type = _asInt(json['type']) ?? 0;
    return CoursewareActivity(
      activityId: _asString(json['id'] ?? json['activityId']),
      coursewareId: _asString(json['courseware_id'] ?? json['coursewareId']),
      title: (json['title'] ?? json['name'] ?? '').toString(),
      type: type,
      typeName: (json['typeName'] ?? _typeName(type)).toString(),
      createTime: _asInt(json['createTime'] ?? json['start_time']) ?? 0,
      finished: json['finished'] == true || _asInt(json['status']) == 1,
    );
  }

  static String _typeName(int type) {
    switch (type) {
      case 1:
      case 14:
        return '课件';
      case 4:
        return '作业';
      default:
        return '活动';
    }
  }

  static String _asString(dynamic v) => v == null ? '' : v.toString();
  static int? _asInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    return int.tryParse(v.toString().split('.').first);
  }
}

class CoursewareSlide {
  final int page;
  final String imageUrl;
  final String? problem;

  CoursewareSlide({
    required this.page,
    required this.imageUrl,
    this.problem,
  });

  factory CoursewareSlide.fromJson(Map<String, dynamic> json) {
    return CoursewareSlide(
      page: CoursewareActivity._asInt(json['page'] ?? json['index']) ?? 1,
      imageUrl: (json['coverAlt'] ?? json['cover'] ?? '').toString(),
      problem: json['problem'] == null ? null : json['problem'].toString(),
    );
  }
}

class CoursewarePresentation {
  final String id;
  final String title;
  final int slidesCount;
  final List<CoursewareSlide> slides;

  CoursewarePresentation({
    required this.id,
    required this.title,
    required this.slidesCount,
    required this.slides,
  });

  factory CoursewarePresentation.fromJson(Map<String, dynamic> json) {
    return CoursewarePresentation(
      id: CoursewareActivity._asString(json['id']),
      title: (json['title'] ?? '').toString(),
      slidesCount: CoursewareActivity._asInt(json['slidesCount']) ?? 0,
      slides: const [],
    );
  }

  CoursewarePresentation withSlides(List<CoursewareSlide> fullSlides) {
    final sorted = [...fullSlides]..sort((a, b) => a.page.compareTo(b.page));
    return CoursewarePresentation(
      id: id,
      title: title,
      slidesCount: slidesCount > 0 ? slidesCount : sorted.length,
      slides: sorted,
    );
  }
}
