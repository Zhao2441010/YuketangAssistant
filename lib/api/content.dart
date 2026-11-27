import 'api_service.dart';

/// 历史课件链路（仅需登录 Cookie，不需要 pptJwt）
///
/// 关键：历史课时必须用 activity.courseware_id 作为 lesson_id，
/// 用 activity.id 会返回 LESSON_DELETED。
class RCContentApi extends Api {
  RCContentApi([super.user]);

  /// 全部课程元数据
  static Future<List<dynamic>?> getLearningList({String? userId}) async {
    final response = await ApiService.sendRequest(
      '/v/course_meta/learning_list/',
      userId: userId,
    );
    if (response == null) return null;
    final data = response.data;
    if (data is List) return data;
    if (data is Map && data['code'] == 0) {
      final payload = data['data'];
      if (payload is List) return payload;
      if (payload is Map && payload['data'] is List) return payload['data'];
    }
    return null;
  }

  /// 某门课的活动列表（type=14 为课堂/课件）
  static Future<List<dynamic>?> getActivities(
    String courseId, {
    String? userId,
    String actype = '-1',
    int offset = 100,
  }) async {
    final response = await ApiService.sendRequest(
      '/v2/api/web/logs/learn/$courseId',
      params: {
        'actype': actype,
        'page': '0',
        'offset': '$offset',
        'sort': '-1',
      },
      userId: userId,
    );
    if (response == null) return null;
    final data = response.data;
    dynamic payload = data is Map ? (data['data'] ?? data['logs']) : data;
    if (payload is Map) {
      payload = payload['logs'] ?? payload['activityList'] ?? payload['list'];
    }
    return payload is List ? payload : null;
  }

  /// 课时下的课件清单
  static Future<List<dynamic>?> fetchPresentations(
    String coursewareId, {
    String? userId,
  }) async {
    final response = await ApiService.sendRequest(
      '/api/v3/lesson-summary/student',
      params: {'lesson_id': coursewareId},
      userId: userId,
    );
    if (response == null) return null;
    final data = response.data;
    final payload = data is Map ? data['data'] : null;
    final list = payload is Map ? payload['presentations'] : null;
    return list is List ? list : null;
  }

  /// 单个课件的全部页（含 problem 题目）
  static Future<List<dynamic>?> fetchSlides(
    String presentationId,
    String coursewareId, {
    String? userId,
  }) async {
    final response = await ApiService.sendRequest(
      '/api/v3/lesson-summary/student/presentation',
      params: {
        'presentation_id': presentationId,
        'lesson_id': coursewareId,
      },
      userId: userId,
    );
    if (response == null) return null;
    final data = response.data;
    final payload = data is Map ? data['data'] : null;
    final list = payload is Map ? payload['slides'] : null;
    return list is List ? list : null;
  }
}
