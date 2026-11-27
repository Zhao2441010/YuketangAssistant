import 'api_service.dart';
import '../models/course.dart';

/// 长江雨课堂课程与课堂接口
class RCCourseApi extends Api {
  RCCourseApi([super.user]);

  // uid -> [bearerToken, lessonToken]
  static final Map<String, List<String>> _tokens = {};

  String? get bearerToken => _tokens[user!.uid]?[0];
  String? get lessonToken => _tokens[user!.uid]?[1];

  void _setToken(String bearerToken, String lessonToken) {
    _tokens[user!.uid] = [bearerToken, lessonToken];
  }

  static Future<Map<String, dynamic>?> getCourses() async {
    final url = '/v/course_meta/learning_list/';
    final response = await ApiService.sendRequest(url);
    return response?.data;
  }

  /// 获取正在上课的课程
  static Future<List<dynamic>?> getOnLesson() async {
    final url = '/api/v3/classroom/on-lesson';
    final response = await ApiService.sendRequest(url);
    if (response?.data['code'] == 0) {
      return response?.data['data']['onLessonClassrooms'];
    }
    return null;
  }

  static Future<List<Course>?> getCoursesList([List<dynamic>? onLessonCourses]) async {
    late Map<String, dynamic>? courses;
    if (onLessonCourses == null) {
      final results = await Future.wait([getCourses(), getOnLesson()]);
      courses = results[0] as Map<String, dynamic>?;
      onLessonCourses = results[1] as List<dynamic>?;
    } else {
      courses = await getCourses();
    }
    if (courses == null || onLessonCourses == null) return null;
    if (courses['data'].isEmpty) return null;

    Map<String, dynamic> coursesMap = {
      for (var courseItem in courses['data'])
        courseItem['course_id'].toString(): courseItem
    };

    List<Course> contentList = [];
    for (var onLessonCourseItem in onLessonCourses) {
      final String courseId = onLessonCourseItem['courseId'];
      if (coursesMap.containsKey(courseId)) {
        final courseItem = coursesMap[courseId];
        courseItem['lesson_id'] = onLessonCourseItem['lessonId'];
        contentList.add(Course.fromRCJson(courseItem));
      }
    }
    return contentList;
  }

  /// 进课堂签到：source=21（扫码跳转），joinIfNotIn=true 自动加入
  Future<int?> checkIn(String lessonId) async {
    final url = '/api/v3/lesson/checkin';
    final jsonData = {
      'source': 21,
      'lessonId': lessonId,
      'joinIfNotIn': true
    };
    final response = await ApiService.sendRequest(
      url, method: 'POST', body: jsonData, userId: user?.uid);
    if (response == null) return null;
    final data = response.data;
    final int code = data['code'];
    if (code == 0) {
      final bearerToken = response.headers.value('set-auth')!;
      final lessonToken = data['data']['lessonToken'];
      _setToken(bearerToken, lessonToken);
      return 0;
    }
    return code;
  }

  /// 动态二维码兜底：服务端解析二维码 URL 换 lessonId
  Future<int?> scan(String qrCodeUrl) async {
    final url = '/api/v3/app/scan';
    final response = await ApiService.sendRequest(
      url,
      method: 'POST',
      body: {'url': qrCodeUrl},
      userId: user?.uid,
    );
    if (response == null) return null;
    final data = response.data;
    final int code = data['code'];
    if (code == 0) {
      final lessonId = data['data']['value'];
      return await checkIn(lessonId);
    }
    return code;
  }

  Future<Map<String, dynamic>?> getCheckIn() async {
    final url = '/api/v3/lesson/location-checkin/eligibility';
    final headers = {'authorization': 'Bearer $bearerToken'};
    final response = await ApiService.sendRequest(
      url, headers: headers, userId: user?.uid);
    if (response == null) return null;
    return response.data['data'];
  }

  Future<Map<String, dynamic>?> submitLocationCheckIn(
      String lessonId, double latitude, double longitude, String address) async {
    final url = '/api/v3/lesson/location-checkin/submit';
    final headers = {'authorization': 'Bearer $bearerToken'};
    final jsonData = {
      'lat': latitude.toString(),
      'lng': longitude.toString(),
      'lessonId': lessonId,
      'source': 27,
      'address': address
    };
    final response = await ApiService.sendRequest(
      url, method: 'POST', headers: headers, body: jsonData, userId: user?.uid);
    return response?.data;
  }

  /// 当前课堂整份 PPT（含未放映页）
  Future<Map<String, dynamic>?> getPresentation(String presentationId) async {
    final url = '/api/v3/lesson/presentation/fetch?presentation_id=$presentationId';
    if (bearerToken == null) return null;
    final headers = {'authorization': 'Bearer $bearerToken'};
    final response = await ApiService.sendRequest(
      url, headers: headers, userId: user?.uid);
    return response?.data['data'];
  }

  /// 课堂答题
  Future<Map<String, dynamic>?> answer(
    String problemId,
    int problemType, {
    bool retry = false,
    int? time,
    List<String>? options,
    String? content,
    List<String>? imageUrls,
  }) async {
    final url = retry
        ? '/api/v3/lesson/problem/retry'
        : '/api/v3/lesson/problem/answer';
    if (bearerToken == null) return null;
    final headers = {'authorization': 'Bearer $bearerToken'};
    final timeStampMS = time ?? DateTime.now().millisecondsSinceEpoch;
    late dynamic result;
    if (problemType == 5) {
      final pics = [
        for (final imageUrl in imageUrls ?? <String>[])
          {'pic': imageUrl, 'thumb': '$imageUrl?imageView2/2/w/568'}
      ];
      if (pics.isEmpty) pics.add({'pic': '', 'thumb': ''});
      result = {'content': content ?? '', 'pics': pics, 'videos': []};
    } else {
      result = options;
    }
    var jsonData = {
      'problemId': problemId,
      'dt': timeStampMS,
      'problemType': problemType,
      'result': result
    };
    if (retry) {
      jsonData = {'problems': [jsonData]};
    }
    final response = await ApiService.sendRequest(
      url, method: 'POST', headers: headers, body: jsonData, userId: user?.uid);
    return response?.data;
  }
}
