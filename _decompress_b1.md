[user]
<acp tokens="13" type="text" time="2026-11-25 12:41:00">m00001</acp>
先读取.md文档,再读取文件信息

[user]
<acp tokens="239" type="text" time="2026-11-25 12:41:00">m00002</acp>
<system-reminder>
The following information describes your current session:

Current working directory: "D:\\workspace\\tools\\YuketangAssistant".

Current DSH file policy: workspace-write. Any available operation enforced by the DSH file sandbox may modify files under the session workspace: "D:\\workspace\\tools\\YuketangAssistant". Some platform temporary areas may also be writable.

Approval prompts are enabled in this session: operations that require approval may ask through the configured answerers; without an available answerer, the request fails closed.

You are the root agent. Your name is "lead".

To message other agents, use send_message({ agent: "<agent-name>", message: "..." }).
Use list_agents({}) to find your collaborator agents and their names.

Current DeepSeek Harness implementation checkout: "D:\\app\\DSHarness\\resources\\app.asar\\dsh\\".

Current DeepSeek Harness Web GUI URL: "http://127.0.0.1:56103".
</system-reminder>

[user]
<acp tokens="66" type="text" time="2026-11-25 12:41:00">m00003</acp>
Time sampled while preparing turn 1, step 1: 2026-11-25T12:41:00+08:00[Asia/Shanghai]
Browser time zone for this request: Asia/Shanghai. Interpret otherwise-unqualified dates and times in this zone.
Elapsed since the preceding model-visible message: unavailable.

[assistant]
The user says "先读取.md文档,再读取文件信息" — first read the .md document, then read the file information. I need to figure out what .md document. Let me look at the working directory.

Let me list files.

[assistant • pwsh]
{"command":"Get-ChildItem -Force | Select-Object Mode, Length, LastWriteTime, Name","description":"List files in working directory"}

[assistant • glob]
{"pattern":"**/*.md"}

[tool • pwsh]
Error: sandbox-local: windows-acl workspace grant failed: Win32Error: SetNamedSecurityInfoW failed (Win32 5): grantWrite(D:\workspace\tools\YuketangAssistant). The command did not run; check that the current user can change permissions on the workspace and temp directories, or run with danger-full-access.

[tool • glob]
node_modules\.pnpm\cross-spawn@7.0.6\node_modules\cross-spawn\README.md
node_modules\.pnpm\which@2.0.2\node_modules\which\CHANGELOG.md
node_modules\.pnpm\which@2.0.2\node_modules\which\README.md
node_modules\.pnpm\path-key@3.1.1\node_modules\path-key\readme.md
node_modules\.pnpm\shebang-command@2.0.0\node_modules\shebang-command\readme.md
node_modules\.pnpm\isexe@2.0.0\node_modules\isexe\README.md
node_modules\.pnpm\shebang-regex@3.0.0\node_modules\shebang-regex\readme.md
node_modules\.pnpm\safer-buffer@2.1.2\node_modules\safer-buffer\Porting-Buffer.md
node_modules\.pnpm\safer-buffer@2.1.2\node_modules\safer-buffer\Readme.md
node_modules\.pnpm\tslib@2.9.1\node_modules\tslib\README.md
node_modules\.pnpm\@tailwindcss+oxide-wasm32-wasi@4.3.5\node_modules\@tailwindcss\oxide-wasm32-wasi\node_modules\tslib\SECURITY.md
node_modules\.pnpm\tslib@2.9.1\node_modules\tslib\SECURITY.md
node_modules\.pnpm\autoprefixer@10.7.0_postcss@8.5.33\node_modules\autoprefixer\README.md
node_modules\.pnpm\postcss@8.5.33\node_modules\postcss\README.md
node_modules\.pnpm\csstype@3.2.4\node_modules\csstype\README.md
node_modules\.pnpm\fraction.js@5.3.4\node_modules\fraction.js\CHANGELOG.md
node_modules\.pnpm\fraction.js@5.3.4\node_modules\fraction.js\README.md
node_modules\.pnpm\@jridgewell+sourcemap-codec@1.6.0\node_modules\@jridgewell\sourcemap-codec\README.md
node_modules\.pnpm\fs-extra@11.4.1\node_modules\fs-extra\README.md
node_modules\.pnpm\source-map-js@1.2.4\node_modules\source-map-js\README.md
node_modules\.pnpm\baseline-browser-mapping@2.11.39\node_modules\baseline-browser-mapping\README.md
node_modules\.pnpm\undici-types@6.21.0\node_modules\undici-types\README.md
node_modules\.pnpm\picomatch@4.0.7\node_modules\picomatch\README.md
node_modules\.pnpm\nanoid@3.3.20\node_modules\nanoid\README.md
node_modules\.pnpm\browserslist@4.29.6\node_modules\browserslist\README.md
node_modules\.pnpm\node-releases@2.0.65\node_modules\node-releases\README.md
node_modules\.pnpm\@isaacs+nonrecursive-glob-match@1.1.0\node_modules\@isaacs\nonrecursive-glob-match\LICENSE.md
node_modules\.pnpm\graceful-fs@4.2.12\node_modules\graceful-fs\LICENSE.md
node_modules\.pnpm\lru-cache@11.5.3\node_modules\lru-cache\LICENSE.md
node_modules\.pnpm\minipass@7.1.3\node_modules\minipass\LICENSE.md
node_modules\.pnpm\path-scurry@3.0.0\node_modules\path-scurry\LICENSE.md
node_modules\.pnpm\rimraf@6.1.4\node_modules\rimraf\LICENSE.md
node_modules\.pnpm\sax@1.6.1\node_modules\sax\LICENSE.md
node_modules\.pnpm\tar@7.5.23\node_modules\tar\LICENSE.md
node_modules\.pnpm\sax@1.6.1\node_modules\sax\README.md
node_modules\.pnpm\debug@4.4.3\node_modules\debug\README.md
node_modules\.pnpm\graceful-fs@4.2.12\node_modules\graceful-fs\README.md
node_modules\.pnpm\detect-libc@2.1.2\node_modules\detect-libc\README.md
node_modules\.pnpm\postcss-value-parser@4.2.0\node_modules\postcss-value-parser\README.md
node_modules\.pnpm\update-browserslist-db@1.4.0_browserslist@4.29.6\node_modules\update-browserslist-db\README.md
node_modules\.pnpm\escalade@3.2.0\node_modules\escalade\readme.md
node_modules\.pnpm\picocolors@1.1.1\node_modules\picocolors\README.md
node_modules\.pnpm\jsonfile@6.2.1\node_modules\jsonfile\README.md
node_modules\.pnpm\universalify@2.0.1\node_modules\universalify\README.md
node_modules\.pnpm\ms@2.1.3\node_modules\ms\license.md
node_modules\.pnpm\ms@2.1.3\node_modules\ms\readme.md
node_modules\.pnpm\caniuse-lite@1.0.30001826\node_modules\caniuse-lite\README.md
node_modules\.pnpm\electron-to-chromium@1.5.492\node_modules\electron-to-chromium\README.md
node_modules\.pnpm\@esbuild+win32-x64@0.25.12\node_modules\@esbuild\win32-x64\README.md
node_modules\.pnpm\@esbuild+win32-x64@0.28.2\node_modules\@esbuild\win32-x64\README.md
node_modules\.pnpm\@vitejs+plugin-react@6.3.0__1359e0c8f92e0774b00b9990f6cfd0d4\node_modules\@vitejs\plugin-react\README.md
node_modules\.pnpm\@types+express@4.17.25\node_modules\@types\express\README.md
node_modules\.pnpm\express@4.22.3\node_modules\express\History.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\CHANGELOG.md
node_modules\.pnpm\express@4.22.3\node_modules\express\Readme.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\README-es.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\README.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\SECURITY.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\skills\dotenv\SKILL.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\skills\dotenvx\SKILL.md
node_modules\.pnpm\@types+react-dom@19.3.0_@types+react@19.3.0\node_modules\@types\react-dom\README.md
node_modules\.pnpm\tinyglobby@0.2.17\node_modules\tinyglobby\README.md
node_modules\.pnpm\debug@2.6.9\node_modules\debug\README.md
node_modules\.pnpm\debug@2.6.9\node_modules\debug\CHANGELOG.md
node_modules\.pnpm\tsx@4.23.15\node_modules\tsx\README.md
node_modules\.pnpm\@types+express-serve-static-core@4.19.9\node_modules\@types\express-serve-static-core\README.md
node_modules\.pnpm\p-retry@4.6.2\node_modules\p-retry\readme.md
node_modules\.pnpm\@rolldown+pluginutils@1.1.1\node_modules\@rolldown\pluginutils\README.md
node_modules\.pnpm\@types+node@22.20.5\node_modules\@types\node\README.md
node_modules\.pnpm\motion-dom@12.43.0\node_modules\motion-dom\LICENSE.md
node_modules\.pnpm\motion-utils@12.39.0\node_modules\motion-utils\LICENSE.md
node_modules\.pnpm\motion@12.43.0_react-dom@19.3.1_react@19.3.1__react@19.3.1\node_modules\motion\LICENSE.md
node_modules\.pnpm\motion@12.43.0_react-dom@19.3.1_react@19.3.1__react@19.3.1\node_modules\motion\README.md
node_modules\.pnpm\lightningcss@1.33.0\node_modules\lightningcss\README.md
node_modules\.pnpm\ms@2.0.0\node_modules\ms\license.md
node_modules\.pnpm\ms@2.0.0\node_modules\ms\readme.md
node_modules\.pnpm\gaxios@7.3.1\node_modules\gaxios\README.md
node_modules\.pnpm\google-auth-library@10.9.1\node_modules\google-auth-library\README.md
node_modules\.pnpm\@types+react@19.3.0\node_modules\@types\react\README.md
node_modules\.pnpm\body-parser@1.20.9\node_modules\body-parser\HISTORY.md
node_modules\.pnpm\body-parser@1.20.9\node_modules\body-parser\README.md
node_modules\.pnpm\google-logging-utils@1.1.3\node_modules\google-logging-utils\README.md
node_modules\.pnpm\vite@8.3.8_@types+node@22.2_ca18f45ae093ca5d67b76bb3f5a3d41f\node_modules\vite\README.md
node_modules\.pnpm\vite@8.3.8_@types+node@22.2_ca18f45ae093ca5d67b76bb3f5a3d41f\node_modules\vite\LICENSE.md
node_modules\.pnpm\gcp-metadata@8.1.2\node_modules\gcp-metadata\README.md
node_modules\.pnpm\node-fetch@3.3.2\node_modules\node-fetch\LICENSE.md
node_modules\.pnpm\node-fetch@3.3.2\node_modules\node-fetch\README.md
node_modules\.pnpm\@protobufjs+utf8@1.1.2\node_modules\@protobufjs\utf8\CHANGELOG.md
node_modules\.pnpm\@protobufjs+utf8@1.1.2\node_modules\@protobufjs\utf8\README.md
node_modules\.pnpm\send@0.19.2\node_modules\send\HISTORY.md
node_modules\.pnpm\send@0.19.2\node_modules\send\README.md
node_modules\.pnpm\send@0.19.2\node_modules\send\SECURITY.md
node_modules\.pnpm\fdir@6.5.0_picomatch@4.0.7\node_modules\fdir\README.md
node_modules\.pnpm\side-channel@1.1.1\node_modules\side-channel\CHANGELOG.md
node_modules\.pnpm\side-channel@1.1.1\node_modules\side-channel\README.md
node_modules\.pnpm\mime-db@1.52.0\node_modules\mime-db\HISTORY.md
node_modules\.pnpm\mime-db@1.52.0\node_modules\mime-db\README.md
node_modules\.pnpm\serve-static@1.16.3\node_modules\serve-static\HISTORY.md
node_modules\.pnpm\serve-static@1.16.3\node_modules\serve-static\README.md
node_modules\.pnpm\qs@6.16.2\node_modules\qs\CHANGELOG.md

(Showing 100 of 417 paths. Full sorted result stored at: C:\Users\Zhao\AppData\Local\Temp\dsh-spill-vmK1Lv\session-47da19afa937\0e6844707cab-glob-results.txt. Use read with offset/limit, or grep this path to search within it.)

[assistant]
The pwsh failed due to sandbox ACL issue. There's a skill `diagnose-windows-sandbox-acl`. But maybe I can just use read/glob without pwsh. The user wants to read the .md document. Node_modules flooded the results. Let me glob for md at root level only. Use pattern "*.md".

[assistant • glob]
{"pattern":"*.md"}

[assistant • glob]
{"pattern":"*"}

[tool • glob]
node_modules\.pnpm\cross-spawn@7.0.6\node_modules\cross-spawn\README.md
node_modules\.pnpm\which@2.0.2\node_modules\which\CHANGELOG.md
node_modules\.pnpm\which@2.0.2\node_modules\which\README.md
node_modules\.pnpm\path-key@3.1.1\node_modules\path-key\readme.md
node_modules\.pnpm\shebang-command@2.0.0\node_modules\shebang-command\readme.md
node_modules\.pnpm\isexe@2.0.0\node_modules\isexe\README.md
node_modules\.pnpm\shebang-regex@3.0.0\node_modules\shebang-regex\readme.md
node_modules\.pnpm\safer-buffer@2.1.2\node_modules\safer-buffer\Porting-Buffer.md
node_modules\.pnpm\safer-buffer@2.1.2\node_modules\safer-buffer\Readme.md
node_modules\.pnpm\tslib@2.9.1\node_modules\tslib\README.md
node_modules\.pnpm\@tailwindcss+oxide-wasm32-wasi@4.3.5\node_modules\@tailwindcss\oxide-wasm32-wasi\node_modules\tslib\SECURITY.md
node_modules\.pnpm\tslib@2.9.1\node_modules\tslib\SECURITY.md
node_modules\.pnpm\autoprefixer@10.7.0_postcss@8.5.33\node_modules\autoprefixer\README.md
node_modules\.pnpm\postcss@8.5.33\node_modules\postcss\README.md
node_modules\.pnpm\csstype@3.2.4\node_modules\csstype\README.md
node_modules\.pnpm\fraction.js@5.3.4\node_modules\fraction.js\CHANGELOG.md
node_modules\.pnpm\fraction.js@5.3.4\node_modules\fraction.js\README.md
node_modules\.pnpm\@jridgewell+sourcemap-codec@1.6.0\node_modules\@jridgewell\sourcemap-codec\README.md
node_modules\.pnpm\fs-extra@11.4.1\node_modules\fs-extra\README.md
node_modules\.pnpm\source-map-js@1.2.4\node_modules\source-map-js\README.md
node_modules\.pnpm\baseline-browser-mapping@2.11.39\node_modules\baseline-browser-mapping\README.md
node_modules\.pnpm\undici-types@6.21.0\node_modules\undici-types\README.md
node_modules\.pnpm\picomatch@4.0.7\node_modules\picomatch\README.md
node_modules\.pnpm\nanoid@3.3.20\node_modules\nanoid\README.md
node_modules\.pnpm\browserslist@4.29.6\node_modules\browserslist\README.md
node_modules\.pnpm\node-releases@2.0.65\node_modules\node-releases\README.md
node_modules\.pnpm\@isaacs+nonrecursive-glob-match@1.1.0\node_modules\@isaacs\nonrecursive-glob-match\LICENSE.md
node_modules\.pnpm\graceful-fs@4.2.12\node_modules\graceful-fs\LICENSE.md
node_modules\.pnpm\lru-cache@11.5.3\node_modules\lru-cache\LICENSE.md
node_modules\.pnpm\minipass@7.1.3\node_modules\minipass\LICENSE.md
node_modules\.pnpm\path-scurry@3.0.0\node_modules\path-scurry\LICENSE.md
node_modules\.pnpm\rimraf@6.1.4\node_modules\rimraf\LICENSE.md
node_modules\.pnpm\sax@1.6.1\node_modules\sax\LICENSE.md
node_modules\.pnpm\tar@7.5.23\node_modules\tar\LICENSE.md
node_modules\.pnpm\sax@1.6.1\node_modules\sax\README.md
node_modules\.pnpm\debug@4.4.3\node_modules\debug\README.md
node_modules\.pnpm\graceful-fs@4.2.12\node_modules\graceful-fs\README.md
node_modules\.pnpm\detect-libc@2.1.2\node_modules\detect-libc\README.md
node_modules\.pnpm\postcss-value-parser@4.2.0\node_modules\postcss-value-parser\README.md
node_modules\.pnpm\update-browserslist-db@1.4.0_browserslist@4.29.6\node_modules\update-browserslist-db\README.md
node_modules\.pnpm\escalade@3.2.0\node_modules\escalade\readme.md
node_modules\.pnpm\picocolors@1.1.1\node_modules\picocolors\README.md
node_modules\.pnpm\jsonfile@6.2.1\node_modules\jsonfile\README.md
node_modules\.pnpm\universalify@2.0.1\node_modules\universalify\README.md
node_modules\.pnpm\ms@2.1.3\node_modules\ms\license.md
node_modules\.pnpm\ms@2.1.3\node_modules\ms\readme.md
node_modules\.pnpm\caniuse-lite@1.0.30001826\node_modules\caniuse-lite\README.md
node_modules\.pnpm\electron-to-chromium@1.5.492\node_modules\electron-to-chromium\README.md
node_modules\.pnpm\@esbuild+win32-x64@0.25.12\node_modules\@esbuild\win32-x64\README.md
node_modules\.pnpm\@esbuild+win32-x64@0.28.2\node_modules\@esbuild\win32-x64\README.md
node_modules\.pnpm\@vitejs+plugin-react@6.3.0__1359e0c8f92e0774b00b9990f6cfd0d4\node_modules\@vitejs\plugin-react\README.md
node_modules\.pnpm\@types+express@4.17.25\node_modules\@types\express\README.md
node_modules\.pnpm\express@4.22.3\node_modules\express\History.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\CHANGELOG.md
node_modules\.pnpm\express@4.22.3\node_modules\express\Readme.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\README-es.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\README.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\SECURITY.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\skills\dotenv\SKILL.md
node_modules\.pnpm\dotenv@17.4.2\node_modules\dotenv\skills\dotenvx\SKILL.md
node_modules\.pnpm\@types+react-dom@19.3.0_@types+react@19.3.0\node_modules\@types\react-dom\README.md
node_modules\.pnpm\tinyglobby@0.2.17\node_modules\tinyglobby\README.md
node_modules\.pnpm\debug@2.6.9\node_modules\debug\README.md
node_modules\.pnpm\debug@2.6.9\node_modules\debug\CHANGELOG.md
node_modules\.pnpm\tsx@4.23.15\node_modules\tsx\README.md
node_modules\.pnpm\@types+express-serve-static-core@4.19.9\node_modules\@types\express-serve-static-core\README.md
node_modules\.pnpm\p-retry@4.6.2\node_modules\p-retry\readme.md
node_modules\.pnpm\@rolldown+pluginutils@1.1.1\node_modules\@rolldown\pluginutils\README.md
node_modules\.pnpm\@types+node@22.20.5\node_modules\@types\node\README.md
node_modules\.pnpm\motion-dom@12.43.0\node_modules\motion-dom\LICENSE.md
node_modules\.pnpm\motion-utils@12.39.0\node_modules\motion-utils\LICENSE.md
node_modules\.pnpm\motion@12.43.0_react-dom@19.3.1_react@19.3.1__react@19.3.1\node_modules\motion\LICENSE.md
node_modules\.pnpm\motion@12.43.0_react-dom@19.3.1_react@19.3.1__react@19.3.1\node_modules\motion\README.md
node_modules\.pnpm\lightningcss@1.33.0\node_modules\lightningcss\README.md
node_modules\.pnpm\ms@2.0.0\node_modules\ms\license.md
node_modules\.pnpm\ms@2.0.0\node_modules\ms\readme.md
node_modules\.pnpm\gaxios@7.3.1\node_modules\gaxios\README.md
node_modules\.pnpm\google-auth-library@10.9.1\node_modules\google-auth-library\README.md
node_modules\.pnpm\@types+react@19.3.0\node_modules\@types\react\README.md
node_modules\.pnpm\body-parser@1.20.9\node_modules\body-parser\HISTORY.md
node_modules\.pnpm\body-parser@1.20.9\node_modules\body-parser\README.md
node_modules\.pnpm\google-logging-utils@1.1.3\node_modules\google-logging-utils\README.md
node_modules\.pnpm\vite@8.3.8_@types+node@22.2_ca18f45ae093ca5d67b76bb3f5a3d41f\node_modules\vite\README.md
node_modules\.pnpm\vite@8.3.8_@types+node@22.2_ca18f45ae093ca5d67b76bb3f5a3d41f\node_modules\vite\LICENSE.md
node_modules\.pnpm\gcp-metadata@8.1.2\node_modules\gcp-metadata\README.md
node_modules\.pnpm\node-fetch@3.3.2\node_modules\node-fetch\LICENSE.md
node_modules\.pnpm\node-fetch@3.3.2\node_modules\node-fetch\README.md
node_modules\.pnpm\@protobufjs+utf8@1.1.2\node_modules\@protobufjs\utf8\CHANGELOG.md
node_modules\.pnpm\@protobufjs+utf8@1.1.2\node_modules\@protobufjs\utf8\README.md
node_modules\.pnpm\send@0.19.2\node_modules\send\HISTORY.md
node_modules\.pnpm\send@0.19.2\node_modules\send\README.md
node_modules\.pnpm\send@0.19.2\node_modules\send\SECURITY.md
node_modules\.pnpm\fdir@6.5.0_picomatch@4.0.7\node_modules\fdir\README.md
node_modules\.pnpm\side-channel@1.1.1\node_modules\side-channel\CHANGELOG.md
node_modules\.pnpm\side-channel@1.1.1\node_modules\side-channel\README.md
node_modules\.pnpm\mime-db@1.52.0\node_modules\mime-db\HISTORY.md
node_modules\.pnpm\mime-db@1.52.0\node_modules\mime-db\README.md
node_modules\.pnpm\serve-static@1.16.3\node_modules\serve-static\HISTORY.md
node_modules\.pnpm\serve-static@1.16.3\node_modules\serve-static\README.md
node_modules\.pnpm\qs@6.16.2\node_modules\qs\CHANGELOG.md

(Showing 100 of 417 paths. Full sorted result stored at: C:\Users\Zhao\AppData\Local\Temp\dsh-spill-vmK1Lv\session-47da19afa937\8a3723411cca-glob-results.txt. Use read with offset/limit, or grep this path to search within it.)

[tool • glob]
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\laya_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\manifest.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\manifest.json
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\prompt_gemma4_12b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\prompt_gemma4_4b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\prompt_gemma4_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\prompt_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\proofreader_small_expert_model_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\scam_detection_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\speech_recognition_small_expert_model_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\summarizer_gemma4_12b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\summarizer_gemma4_4b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\summarizer_gemma4_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\summarizer_nano_v3_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\summarizer_small_expert_model_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\writing_assistance_gemma4_12b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\writing_assistance_gemma4_4b_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\writing_assistance_gemma4_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\writing_assistance_solution_config.binarypb
.chrome-debug-profile\OptimizationGuideModelsManifest\1.20261018.2\_metadata\verified_contents.json
android\.gitignore
android\app\.gitignore
android\app\build\.npmkeep
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable\ic_launcher_background.xml
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-land-hdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-land-mdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-land-xhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-land-xxhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-land-xxxhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-port-hdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-port-mdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-port-xhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-port-xxhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-port-xxxhdpi-v4\splash.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\drawable-v24\ic_launcher_foreground.xml
android\app\build\intermediates\packaged_res\debug\packageDebugResources\layout\activity_main.xml
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-anydpi-v26\ic_launcher.xml
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-anydpi-v26\ic_launcher_round.xml
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-hdpi-v4\ic_launcher.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-hdpi-v4\ic_launcher_foreground.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-hdpi-v4\ic_launcher_round.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-mdpi-v4\ic_launcher.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-mdpi-v4\ic_launcher_foreground.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-mdpi-v4\ic_launcher_round.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xhdpi-v4\ic_launcher.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xhdpi-v4\ic_launcher_foreground.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xhdpi-v4\ic_launcher_round.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxhdpi-v4\ic_launcher.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxhdpi-v4\ic_launcher_foreground.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxhdpi-v4\ic_launcher_round.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxxhdpi-v4\ic_launcher.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxxhdpi-v4\ic_launcher_foreground.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\mipmap-xxxhdpi-v4\ic_launcher_round.png
android\app\build\intermediates\packaged_res\debug\packageDebugResources\xml\file_paths.xml
android\app\proguard-rules.pro
android\app\src\androidTest\java\com\getcapacitor\myapp\ExampleInstrumentedTest.java
android\app\src\main\res\drawable\ic_launcher_background.xml
android\app\src\main\res\drawable\splash.png
android\app\src\main\res\drawable-land-hdpi\splash.png
android\app\src\main\res\drawable-land-mdpi\splash.png
android\app\src\main\res\drawable-land-xhdpi\splash.png
android\app\src\main\res\drawable-land-xxhdpi\splash.png
android\app\src\main\res\drawable-land-xxxhdpi\splash.png
android\app\src\main\res\drawable-port-hdpi\splash.png
android\app\src\main\res\drawable-port-mdpi\splash.png
android\app\src\main\res\drawable-port-xhdpi\splash.png
android\app\src\main\res\drawable-port-xxhdpi\splash.png
android\app\src\main\res\drawable-port-xxxhdpi\splash.png
android\app\src\main\res\drawable-v24\ic_launcher_foreground.xml
android\app\src\main\res\layout\activity_main.xml
android\app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml
android\app\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml
android\app\src\main\res\mipmap-hdpi\ic_launcher.png
android\app\src\main\res\mipmap-hdpi\ic_launcher_foreground.png
android\app\src\main\res\mipmap-hdpi\ic_launcher_round.png
android\app\src\main\res\mipmap-mdpi\ic_launcher.png
android\app\src\main\res\mipmap-mdpi\ic_launcher_foreground.png
android\app\src\main\res\mipmap-mdpi\ic_launcher_round.png
android\app\src\main\res\mipmap-xhdpi\ic_launcher.png
android\app\src\main\res\mipmap-xhdpi\ic_launcher_foreground.png
android\app\src\main\res\mipmap-xhdpi\ic_launcher_round.png
android\app\src\main\res\mipmap-xxhdpi\ic_launcher.png
android\app\src\main\res\mipmap-xxhdpi\ic_launcher_foreground.png
android\app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png
android\app\src\main\res\mipmap-xxxhdpi\ic_launcher.png
android\app\src\main\res\mipmap-xxxhdpi\ic_launcher_foreground.png
android\app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png
android\app\src\main\res\values\ic_launcher_background.xml
android\app\src\main\res\values\styles.xml
android\app\src\main\res\xml\file_paths.xml
android\app\src\test\java\com\getcapacitor\myapp\ExampleUnitTest.java
android\build.gradle
android\capacitor-cordova-android-plugins\src\main\java\.gitkeep
android\capacitor-cordova-android-plugins\src\main\res\.gitkeep
android\gradle\wrapper\gradle-wrapper.jar
android\gradle\wrapper\gradle-wrapper.properties
android\gradle.properties
android\gradlew
android\gradlew.bat

(Showing 100 of 13115 paths. Full sorted result stored at: C:\Users\Zhao\AppData\Local\Temp\dsh-spill-vmK1Lv\session-47da19afa937\5651ad0fd8e1-glob-results.txt. Use read with offset/limit, or grep this path to search within it.)

[assistant]
Glob doesn't seem to anchor root-level patterns. It seems the pattern "*.md" matched at any depth. Let me search the full sorted result file for root-level entries, or just try reading likely files. Actually the glob result is sorted by modification time. Let me grep the spilled file for lines without backslash.

[assistant • grep]
{"pattern":"^[^\\\\]+$","path":"C:\\Users\\Zhao\\AppData\\Local\\Temp\\dsh-spill-vmK1Lv\\session-47da19afa937\\5651ad0fd8e1-glob-results.txt"}

[assistant • grep]
{"pattern":"^[^\\\\]+\\.md$","path":"C:\\Users\\Zhao\\AppData\\Local\\Temp\\dsh-spill-vmK1Lv\\session-47da19afa937\\8a3723411cca-glob-results.txt"}

[tool • grep]
Found 22 matches

C:\Users\Zhao\AppData\Local\Temp\dsh-spill-vmK1Lv\session-47da19afa937\5651ad0fd8e1-glob-results.txt
Line 1566: .env.example
Line 1567: metadata.json
Line 1619: tsconfig.json
Line 1634: makeup.html
Line 9022: index.html
Line 9023: vite.config.ts
Line 9025: 雨课堂扫码协议-调研记录.md
Line 9027: 长江雨课堂-真实登录与抓包实测.md
Line 9051: README.md
Line 9843: auth-capture.log
Line 10217: .wbapp_nUHkTVcuk4od3SgflkzyeB.genie
Line 11941: pnpm-lock.yaml
Line 11945: package.json
Line 11946: capacitor.config.ts
Line 12797: .gitignore
Line 12798: ykt-logs.tar.gz
Line 12801: 日志获取.md
Line 12845: 正确实现链路.md
Line 12896: 尝试策略.md
Line 12969: fullscreen.js
Line 12974: 技术说明.md
Line 12975: bddeploy.sh

[tool • grep]
Found 7 matches

C:\Users\Zhao\AppData\Local\Temp\dsh-spill-vmK1Lv\session-47da19afa937\8a3723411cca-glob-results.txt
Line 298: 雨课堂扫码协议-调研记录.md
Line 299: 长江雨课堂-真实登录与抓包实测.md
Line 301: README.md
Line 412: 日志获取.md
Line 413: 正确实现链路.md
Line 414: 尝试策略.md
Line 417: 技术说明.md

[assistant]
There are multiple md docs at root: 雨课堂扫码协议-调研记录.md, 长江雨课堂-真实登录与抓包实测.md, README.md, 日志获取.md, 正确实现链路.md, 尝试策略.md, 技术说明.md.

The user said "先读取.md文档,再读取文件信息" — first read the .md doc, then read file information. Ambiguous which doc. Likely the most recently modified one, or all of them. Let me check the modification order — glob returns modification-time order. The listing order in 5651 file isn't ordered? It said "Showing 100 of..." sorted by modification time. Hmm.

Let me check the .md result file order: line 298 雨课堂扫码协议-调研记录.md, 299 长江雨课堂-真实登录与抓包实测.md, 301 README.md, 412 日志获取.md, 413 正确实现链路.md, 414 尝试策略.md, 417 技术说明.md. If sorted by mtime descending, the first is most recent.

I'll read them all — they're probably the project docs. Let me read the two most important first. Actually to be safe, read all 7 but they may be long. Let me read and see sizes. No pwsh due to ACL failure. Hmm, pwsh fails entirely due to sandbox ACL grant issue. That's a problem for "文件信息" (file information). I may need to fix sandbox ACL using the diagnose skill, or use danger-full-access retry.

Let me first read the md files.

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\雨课堂扫码协议-调研记录.md"}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\长江雨课堂-真实登录与抓包实测.md"}

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\雨课堂扫码协议-调研记录.md</path>
<type>file</type>
<content>
1: # 雨课堂扫码登录协议调研记录
2: 
3: > 调研时间：2026-11-23　|　结论：**可生成真实可用的扫码链接服务**（二维码生成部分已实测通过）
4: > 本文档记录联网检索到的协议细节与本机实测结果，供后续 agent 直接读取，避免重复调研。
5: 
6: ---
7: 
8: ## 1. 实测结论（本机网络，2026-11-23）
9: 
10: | 端点 | 结果 | 说明 |
11: |---|---|---|
12: | `https://www.yuketang.cn/web` | HTTP 200 | 标准区域网页版可访问 |
13: | `GET https://changjiang.ykt.io/custom/auth/v1/auth/status?ticket=invalid&format=json` | **HTTP 404** | 博客中的 HTTP 长轮询端点实测失效/路径不完整，**不要直接用它** |
14: | `wss://www.yuketang.cn/wsapp/`（WebSocket） | **连接成功并返回真实二维码 ticket** | 这是当前可用的扫码登录入口 |
15: | `https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=...` | HTTP 200，image/jpg，约 39KB | 真实二维码图片可下载 |
16: | Java 21 `java.net.http.WebSocket` 客户端 | 实测成功 | 无需第三方依赖；需放宽握手超时到 20s |
17: 
18: 一次真实响应样例（隐私字段已保留结构）：
19: 
20: ```json
21: {
22:   "op": "requestlogin",
23:   "loginid": "ac7610a5-3e64-45ab-93eb-9e89ebb9c231",
24:   "expire_seconds": 60,
25:   "ticket": "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=gQG38DwAAAA...",
26:   "qrcode": "http://weixin.qq.com/q/02V00ZFJ7jds11eVqcNHcf",
27:   "isReply": true
28: }
29: ```
30: 
31: ---
32: 
33: ## 2. 真实协议流程（WebSocket，实测可用）
34: 
35: 1. **建立连接**：`wss://www.yuketang.cn/wsapp/`，带 Header `Origin: https://www.yuketang.cn`
36: 2. **请求二维码**（客户端发送）：
37:    ```json
38:    {"op":"requestlogin","role":"web","version":1.4,"type":"qrcode","from":"web"}
39:    ```
40:    > 官方偶发"需客户端重发才响应"，实现中做了最多 3 次重试。
41: 3. **服务端返回** `op=requestlogin` 消息，含 `loginid`、`ticket`（二维码图片 URL）、`expire_seconds=60`
42: 4. **下载二维码**：GET `ticket` URL，得到 JPG 图片，转 base64 data URL 展示给用户
43: 5. **用户微信扫码并在手机确认** —— 这一步必须由真人完成，无法自动化
44: 6. **同一 WebSocket 收到确认消息**：
45:    ```json
46:    {"op":"loginsuccess","subscribe_status":true,"UserID":123456,"Auth":"...","loginid":"..."}
47:    ```
48: 7. **换取登录态**：
49:    - `GET https://www.yuketang.cn/v/course_meta/user_info` → 从 Set-Cookie 取 `csrftoken`
50:    - `POST https://www.yuketang.cn/pc/web_login`，Header `Cookie: csrftoken=...`、`X-CSRFToken: ...`，Body `{"UserID":<数字>,"Auth":"<Auth值>"}`
51:    - 从响应 Set-Cookie 取 `sessionid` —— 这就是后续业务接口的凭据
52: 
53: ### 配套业务接口（公开项目整理，未逐一实测）
54: 
55: - 课程列表：`GET https://www.yuketang.cn/v2/api/web/courses/list?identity=2`（需 sessionid Cookie）
56: 
57: ---
58: 
59: ## 3. 四个区域（会话互不通用）
60: 
61: 来源：`@powercess/yuketang-mcp`、`ahdaolma-yuketang-mcp`（LobeHub / Glama）
62: 
63: | region | 名称 | 说明 |
64: |---|---|---|
65: | `standard` | 雨课堂 | 默认，对应 `www.yuketang.cn`，本实现默认使用 |
66: | `hetang` | 荷塘雨课堂 | 需切换域名 |
67: | `changjiang` | 长江雨课堂 | 对应 `changjiang.ykt.io` / `changjiang.yuketang.cn` |
68: | `huanghe` | 黄河雨课堂 | 需切换域名 |
69: 
70: > 若你的学校账号属于非 `standard` 区域，需修改 `application.yml` 的 `gongxiaohu.ykt-gateway.base-url` 与 `ws-url` 为该区域域名。会话不跨区域通用。
71: 
72: ---
73: 
74: ## 4. 资料来源
75: 
76: - 工小湖博客（HTTP 长轮询 + join_lesson 签名链接）：<https://blog.jiclub.site/2026/yuketang-deep-dive>
77: - 博客园《雨课堂模拟登录_获取websocket数据》：**完整的 WebSocket 登录流程与代码**：<https://www.cnblogs.com/xuanyu-10-18/p/13797159.html>
78: - GitHub `Sdl-Jenny/ykt`（可运行的 WebSocket 扫码登录 Python 代码）：<https://github.com/Sdl-Jenny/ykt/blob/main/ykt.py>
79: - `@powercess/yuketang-mcp`（区域划分、sessionid 会话模型）：<https://github.com/powercess/yuketang-mcp>
80: - `ahdaolma-yuketang-mcp`（区域差异、扫码实测提示）：<https://lobehub.com/pt-BR/mcp/ahdaolma-yuketang-mcp>
81: - `Zachary709/YuKeTang`（Python WebSocket 登录实现）：<https://github.com/Zachary709/YuKeTang>
82: 
83: ---
84: 
85: ## 5. 仍需用户提供/确认的材料
86: 
87: 真实服务**已能生成二维码并等待确认**，但要打通"扫码后拿到 sessionid 并访问业务数据"的完整链路，需要你提供以下任一项完成最终验证：
88: 
89: 1. **一次真人扫码确认**（推荐）：启动 `gongxiaohu.mode: real` 的后端，用你的微信扫页面上的真实二维码并确认，我会根据服务端返回的 `loginsuccess` 字段校验 `UserID/Auth → sessionid` 换取流程是否与你学校区域一致。
90: 2. **学校所属区域**：standard / hetang / changjiang / huanghe。若非 standard，需提供对应域名（可由浏览器开发者工具查看登录页的 WebSocket 连接地址）。
91: 3. 若扫码后 `web_login` 换取 sessionid 失败（不同学校可能有字段差异），提供一次**浏览器开发者工具 Network 面板中 `/pc/web_login` 的请求与响应**（可打码 UserID/Auth/sessionid 值，保留字段名与结构）。
92: 4. 若要打通答题/签到等写操作，需提供对应课堂接口的抓包（本仓库当前仅有免扫码进班与超时补答的模拟实现）。

(End of file - total 92 lines)
</content>

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\长江雨课堂-真实登录与抓包实测.md</path>
<type>file</type>
<content>
1: # 长江雨课堂 · 真实登录与抓包实测记录
2: 
3: > 实测时间：2026-11-23 14:12–14:20　|　账号：真实长江雨课堂账号（学号 55120220，华中农业大学）
4: > 本文档记录**真实扫码登录成功**与**真实抓包数据**，供后续 agent 直接读取。敏感值（sessionid、手机号）已部分脱敏。
5: 
6: ---
7: 
8: ## 1. 结论
9: 
10: ✅ **真实扫码登录已成功**：长江雨课堂账号通过 WebSocket 扫码登录，后端拿到 `userId=55120220`、`sessionid`（真实）且 `mock=false`。
11: ✅ **真实课表已抓到**：7 门 2026 秋课程，接口 `/v2/api/web/courses/list?identity=2`。
12: ✅ **真实交题接口链路已抓包验证**：章节 → leaf_info → 题目列表 → `problem_apply` 提交，全部返回 `success=true`。
13: ✅ **真实题目已抓到**：形势与政策（二）时评作业，1 道 ShortAnswer 主观题。
14: 
15: ---
16: 
17: ## 2. 配置修改（已落地）
18: 
19: `backend/src/main/resources/application.yml`：
20: 
21: ```yaml
22: gongxiaohu:
23:   mode: real                                  # 已由 mock 改为 real
24:   ykt-gateway:
25:     region: changjiang                        # standard|hetang|changjiang|huanghe
26:     base-url: https://changjiang.yuketang.cn  # 原 www.yuketang.cn
27:     ws-url: wss://changjiang.yuketang.cn/wsapp/
28:     endpoints:
29:       course-list: /v2/api/web/courses/list   # ✅实测可用（原假路径已废弃）
30:       live-list: /v2/api/web/courses/live     # 需 classroom_id，待补
31: ```
32: 
33: **域名实测**：`changjiang.yuketang.cn` 与 `changjiang.ykt.io` 的 `/wsapp/` 都能返回真实二维码；采用学校官方指引的 `changjiang.yuketang.cn`。
34: 
35: ---
36: 
37: ## 3. 登录链路实测数据
38: 
39: - WebSocket：`wss://changjiang.yuketang.cn/wsapp/`，发 `requestlogin` → 收到真实 `loginid` + `ticket`（微信二维码） + `expire_seconds=60`
40: - 扫码确认后收到 `loginsuccess`：`UserID=55120220`、`Auth=<脱敏>`、`subscribe_status=true`
41: - `GET /v/course_meta/user_info` 返回真实档案：`user_id=55120220`、`name=赵**、school=华中农业大学`、`phone_number=153****0780`
42: - `POST /pc/web_login` 返回 `Set-Cookie: sessionid=<真实值>` → 登录成功
43: - 验证 sessionid：`GET /v2/api/web/courses/list?identity=2` 返回 200 + 真实课表
44: 
45: > 后端本地 token：`GXH-c19c5...`（绑定 sessionid，2 小时有效）
46: 
47: ---
48: 
49: ## 4. 真实课表结构（关键字段）
50: 
51: `GET https://changjiang.yuketang.cn/v2/api/web/courses/list?identity=2`
52: Cookie: `sessionid=<...>`
53: 
54: ```json
55: {"errcode":0,"errmsg":"Success","data":{"list":[
56:   {"term":202601,"name":"2026秋-[主修课程]机器学习导引-0002(...)","classroom_id":25525873,
57:    "teacher":{"user_id":31401801,"name":"邵佳","avatar":"..."},
58:    "course":{"name":"机器学习导引","university_id":2803,"university_name":"华中农业大学","is_pro":true},
59:    "time":1786228075000,"color_code":1,"students_count":109,"role":5}
60: ]}}
61: ```
62: 
63: 真实课程清单（7 门）：
64: 
65: | 课程 | classroom_id | 教师 | 备注 |
66: |---|---|---|---|
67: | 机器学习导引 | 25525873 | 邵佳 | |
68: | 操作系统A | 25525972 | 任继平 | |
69: | 计算机网络A | 25525973 | 王建勇 | |
70: | 接口技术 | 25525977 | 高俊祥 | |
71: | 接口技术A | 25525978 | 高俊祥 | |
72: | 操作系统实验 | 25526051 | 任继平 | |
73: | 形势与政策（二） | 25529399 | — | **含测试题** |
74: 
75: > 注意：课表接口**无 `status` 字段**，不能用它判断"正在上课"。`live-list` 类接口需要 `classroom_id` 参数，待进一步抓包。
76: 
77: ---
78: 
79: ## 5. 交题（课后测试题）抓包结果 ✅
80: 
81: 来源：`Zachary709/YuKeTang` 源码 + 本人真实账号实测。
82: 
83: ### 5.1 完整链路
84: 
85: | 步骤 | 接口 | 实测结果 |
86: |---|---|---|
87: | 1. 章节 | `GET /mooc-api/v1/lms/learn/course/chapter?cid={cid}&term=latest&uv_id={uv}&classroom_id={cid}` | success=true，机器学习导引 1 章、形势与政策 1 章 |
88: | 2. leaf_info | `GET /mooc-api/v1/lms/learn/leaf_info/{cid}/{leaf_id}/` | success=true，`leaf_type_id=4000000` |
89: | 3. 题目列表 | `GET /mooc-api/v1/lms/exercise/get_exercise_list/{leaf_type_id}/` | success=true，1 道题 |
90: | 4. 提交 | `POST /mooc-api/v1/lms/exercise/problem_apply/` | 已实现，协议见下 |
91: 
92: 必需 Header（全套）：
93: ```
94: Cookie: sessionid=<...>
95: classroom-id: 25529399
96: university-id: 2803
97: uv-id: 2803
98: xtbz: ykt
99: x-client / xt-agent: web
100: accept: application/json, text/plain, */*
101: x-csrftoken: <从 /v/course_meta/user_info 的 Set-Cookie 取>
102: ```
103: 
104: ### 5.2 提交 payload 格式（关键）
105: 
106: ```json
107: // 单选/多选/判断题：answer 数组
108: {"classroom_id":25529399,"problem_id":39145902,"answer":["A","B"]}
109: 
110: // 填空题：answers 字典，key 从 "1" 开始
111: {"classroom_id":25529399,"problem_id":39145902,"answers":{"1":"答案1","2":"答案2"}}
112: ```
113: 
114: ### 5.3 真实题目样例（形势与政策（二））
115: 
116: ```
117: problem_id = 39145902
118: content.Type = ShortAnswer（主观题）
119: content.Score = 100.0
120: content.Body = <div class="custom_ueditor_cn_body">...</div>
121: content.Answer = ""
122: user.my_answer.content = "两百块钱与一棵树 ——从"农民养老金倍增"想到秭归山坡上的那些人 ..."（已作答样例）
123: data.font = https://fe-static-yuketang.yuketang.cn/fe_font/product/exam_font_2353b9e99dd84b4d984111d9ca2bdfa1.ttf  ← 字体混淆
124: ```
125: 
126: > `ShortAnswer` 题型未在上述提交协议中覆盖（源码仅处理 SingleChoice/MultipleChoice/判断/FillBlank），需补抓主观题提交格式。
127: 
128: ---
129: 
130: ## 6. 已落地的代码
131: 
132: - `YktWebSocketLogin`：长江域名扫码登录（已改为 `changjiang.yuketang.cn`）
133: - `RealYktGateway.fetchCourseList`：已适配真实字段（`data.list`、`teacher.name`、`course.name`、`classroom_id`）
134: - `YktExerciseApi`：**新增**，实现上述交题链路与 payload 格式（单选/多选/判断/填空）
135: - `application.yml`：mode=real、region=changjiang、真实课表端点
136: 
137: ---
138: 
139: ## 7. 仍需提供 / 后续工作
140: 
141: 1. **正在上课判定**：课表接口无 status，需抓包"正在上课"列表接口（可能需要 classroom_id 或校园广播接口）。
142: 2. **主观题（ShortAnswer）提交格式**：抓包一次主观题提交请求（Body + Header）。
143: 3. **随堂超时补答**：本次抓的是"课后测试题"，与博客所述"随堂题超时补交"不同。若要做随堂补交，需在老师发题时抓包对应提交接口。
144: 4. **图片上传题**：需抓包草稿图上传接口。

(End of file - total 144 lines)
</content>

[assistant]
Now read the other docs: README.md, 技术说明.md, 正确实现链路.md, 尝试策略.md, 日志获取.md. Plus "再读取文件信息" — read file information (project structure). Since pwsh is failing due to ACL, I should try to diagnose. But maybe I can use glob results to understand structure with read of key files (package.json, metadata.json, etc.).

Let me read remaining md files first, in parallel.

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\README.md"}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\技术说明.md"}

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\README.md</path>
<type>file</type>
<content>
1: > # ⚠️ 重要声明
2: >
3: > **本项目的前端与后端均由 AI 开发完成。**
4: >
5: > **仅供个人学习与使用，不进行任何商业化，不对外提供服务，也不用于任何扰乱教学秩序的场景。**
6: >
7: > 请遵守你所在学校与平台的相关规定，因不当使用产生的后果由使用者自行承担。
8: 
9: # YuketangAssistant
10: 
11: 一个本地运行的雨课堂辅助工具：登录后可查看课程与课件、提前获取课件内嵌题目、在老师未收题时完成随堂补答。
12: 
13: 本项目**受工小湖项目启发**，参考其公开技术博客中关于抓包分析与功能设计的思路，独立实现了本项目的前后端。
14: 
15: ---
16: 
17: ## 功能特性
18: 
19: - **两种登录方式**
20:   - 微信扫码登录（45 秒阻塞长轮询）
21:   - 账号密码登录（密码 RSA 加密 + 腾讯滑块验证）
22:   - 会话自动续期，token 过期无感重连
23: - **免扫码进班**
24:   - 自动识别 `status=1` 的正在上课课程
25:   - HMAC-SHA256 签名进班链接
26: - **随堂超时补答**
27:   - 支持单选、多选、填空、图片上传等题型
28:   - 倒计时结束但老师未收题时仍可补交，重复提交幂等覆盖
29:   - 草稿本地持久化、最多 9 张图片直传
30: - **课程中心（三级下钻）**
31:   - 全部课程 → 课时列表 → 课件与题目
32:   - 历史课件获取（按 `courseware_id` 定位课件）
33:   - 课件与测验分层解耦：教师题目集中在顶部测验区，点击题目跳转对应课件页
34:   - PPT 浏览：缩略图侧边预览快速跳转、滚轮翻页（不滚动页面）、页码输入框跳转
35: - **题目预测**
36:   - 从课件页的 `problem` 字段提前提取题目
37:   - 展示题干、选项、参考答案，并高亮正确选项
38: 
39: ---
40: 
41: ## 技术栈
42: 
43: | 层次   | 选型                                               |
44: | ---- | ------------------------------------------------ |
45: | 前端   | React 19 + Vite 8 + TypeScript + Tailwind CSS v4 |
46: | 后端   | Java 21 + Spring Boot 3.4.5 + Maven              |
47: | 运行模式 | `mock`（内置演示数据）/ `real`（真实雨课堂接口）                  |
48: 
49: ---
50: 
51: ## 快速开始
52: 
53: ### 1. 启动后端
54: 
55: 用 IDE 打开 `backend/` 目录，运行 `GongXiaoHuApplication`（默认 `http://localhost:8080`）。
56: 
57: 或使用命令行：
58: 
59: ```bash
60: cd backend
61: mvn -DskipTests spring-boot:run
62: ```
63: 
64: 配置见 `backend/src/main/resources/application.yml`：
65: 
66: - `gongxiaohu.mode: mock` —— 无需账号即可体验完整流程
67: - `gongxiaohu.mode: real` —— 使用真实雨课堂接口
68: 
69: ### 2. 启动前端
70: 
71: ```bash
72: pnpm install
73: pnpm dev
74: ```
75: 
76: 浏览器打开 <http://localhost:3000/>。
77: 
78: ### 3. 运行测试
79: 
80: ```bash
81: cd backend && mvn test     # 后端测试
82: cd .. && pnpm build        # 前端构建
83: ```
84: 
85: ---
86: 
87: ## 项目结构
88: 
89: ```
90: .
91: ├── index.html / makeup.html   # 前端入口（均指向补答应用）
92: ├── src/
93: │   └── makeup/
94: │       ├── main.tsx           # 渲染入口（支持 ?token=&sid= 免扫码）
95: │       ├── api.ts             # 后端接口封装与类型定义
96: │       ├── LoginView.tsx      # 扫码 / 账号密码登录页
97: │       ├── MakeupApp.tsx      # 上课课程列表 + 随堂补答页
98: │       └── CourseCenter.tsx   # 课程中心（课程→课时→课件/题目）
99: ├── backend/
100: │   └── src/main/java/com/jiclub/gongxiaohu/
101: │       ├── config/            # 配置绑定与 CORS
102: │       ├── gateway/           # 网关抽象 + Mock/Real 实现 + 登录/课件/随堂题协议
103: │       ├── service/           # 会话、进班、补答、随堂题业务
104: │       ├── controller/        # /api/v1 REST 接口
105: │       ├── model/             # 领域模型（record）
106: │       └── util/              # 签名工具
107: └── 技术说明.md                # 开发交接文档（含实现细节与验证记录）
108: ```
109: 
110: ---
111: 
112: ## 主要接口
113: 
114: | 接口                                                                       | 说明               |
115: | ------------------------------------------------------------------------ | ---------------- |
116: | `GET /api/v1/auth/qr` · `POST /auth/poll`                                | 扫码登录             |
117: | `POST /api/v1/auth/password-login`                                       | 账号密码登录           |
118: | `POST /api/v1/auth/import-session`                                       | 用 sessionid 导入会话 |
119: | `GET /api/v1/courses`                                                    | 课程列表与上课状态        |
120: | `GET /api/v1/inclass/quizzes` · `POST /inclass/answers`                  | 随堂题获取与补交         |
121: | `GET /api/v1/content/activities` · `/content/lesson` · `/content/slides` | 课程中心三级数据         |
122: | `GET /api/v1/preview/quizzes`                                            | 课件题目预览           |
123: 
124: 完整接口与 curl 示例见 `backend/README.md`。
125: 
126: ---
127: 
128: ## 隐私说明
129: 
130: - 登录凭据与 token 仅保存在浏览器 localStorage 与后端内存中，不写入数据库；
131: - 日志中的学号、token 均做掩码处理；
132: - 关闭后端进程后内存会话即失效。
133: 
134: ---
135: 
136: ## 参考资料
137: 
138: - 工小湖技术博客：《工小湖：给雨课堂和学校教务做了个小程序》<https://blog.jiclub.site/2026/yuketang-deep-dive>

(End of file - total 138 lines)
</content>

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\技术说明.md</path>
<type>file</type>
<content>
1: # 雨课堂助手 · 技术说明
2: 
3: > 本文档记录本工作区（React + Spring Boot 双端）的**实际架构、真实实现的功能协议、抓包分析方法与接口清单**。仅供个人学习使用。源码工作区：`D:/workspace/tools/YuketangAssistant`。
4: 
5: ---
6: 
7: ## 1. 项目概览
8: 
9: 一个本地运行的雨课堂辅助工具：登录后可查看课程与历史课件、提前获取课件内嵌题目、在老师未收题时完成随堂补答。**前端与后端均已实现真实协议**（非演示数据），`mode: real` 直连长江雨课堂，`mode: mock` 可用内置数据离线体验。
10: 
11: **技术栈**
12: 
13: | 层次 | 选型                                                              |
14: | -- | --------------------------------------------------------------- |
15: | 前端 | **微信小程序**（原生 WXML/WXSS/JS，`miniprogram/`）+ React 19 Web 版（保留） |
16: | 后端 | Java 21 + Spring Boot 3.4.5 + Maven                             |
17: | 运行 | 小程序用 WorkBuddy 应用卡预览；Web 端 `pnpm dev`（:3000）；后端 `mvn spring-boot:run`（:8080） |
18: 
19: **启动**：`backend/src/main/resources/application.yml` 中 `gongxiaohu.mode` 控制 real/mock，`region: changjiang`、`base-url: https://changjiang.yuketang.cn`。
20: 
21: ---
22: 
23: ## 2. 项目结构（当前启用部分）
24: 
25: ```
26: .
27: ├── miniprogram/                  # 【新增】微信小程序（连现有 Java 后端）
28: │   ├── app.js / app.json / app.wxss
29: │   ├── utils/api.js              # 后端 API 封装（token/sessionid 本地缓存 + 401 无感重导）
30: │   └── pages/                    # login / index(上课中) / courses / activities / slides / quiz
31: ├── index.html / makeup.html      # Web 前端入口（保留）
32: ├── src/
33: │   ├── makeup/
34: │   │   ├── main.tsx              # 渲染入口（支持 ?token=&sid= 免扫码）
35: │   │   ├── api.ts                # 后端接口封装 + 类型定义
36: │   │   ├── LoginView.tsx         # 扫码 / 账号密码登录页
37: │   │   ├── MakeupApp.tsx         # 课程列表 + 随堂补答页
38: │   │   └── CourseCenter.tsx      # 课程中心（课程→课时→课件PPT+题目）
39: │   └── index.css                 # Tailwind + 全局样式
40: ├── backend/src/main/java/com/jiclub/gongxiaohu/
41: │   ├── config/                   # 配置绑定 + CORS
42: │   ├── gateway/                  # YktGateway 抽象 + Mock/Real 实现
43: │   │                             # YktWebSocketLogin（扫码）/ YktPasswordLoginApi（密码）
44: │   │                             # YktContentApi（课件）/ YktInClassQuizApi（随堂题）/ YktExerciseApi（交题）
45: │   ├── service/                  # 会话、进班、补答、随堂题业务
46: │   ├── controller/               # /api/v1 REST 接口
47: │   ├── model/                    # 领域模型（record）
48: │   └── util/SignUtil             # 签名工具
49: └── 技术说明.md / README.md / backend/README.md
50: ```
51: 
52: > 已停用的历史文件（`src/App.tsx`、`src/components/*`、`src/data/*`）不再被任何入口引用、不参与构建，源码仅作保留。
53: 
54: ---
55: 
56: ## 3. 已实现功能与真实协议
57: 
58: ### 3.1 登录与会话
59: 
60: - **扫码登录**：`YktWebSocketLogin` 走实测 WebSocket 协议——连 `wss://changjiang.yuketang.cn/wsapp/` → `requestlogin` → 真实二维码 → `loginsuccess`(UserID/Auth) → `/pc/web_login` 换 sessionid。二维码 60 秒、轮询 45 秒。
61:   - ⚠️ **关键 bug 修复（11-24 12:43，真机扫码实测验证）**：原 `handleMessage` 让 `subscribe_status`（订阅状态通知，常在 loginsuccess 前到达且**不含 Auth**）也 complete `loginFuture`。导致 poll 拿到无 Auth 消息 → 返回空，而 future 已无法二次 complete → 真正的 `loginsuccess` 被丢弃 → **扫码成功却永远登不上**。修复为只认含 Auth 的消息才 complete。真机实测：扫码后日志出现「收到登录成功凭据 op=loginsuccess」，poll 返回 `code=0` + token/userId/sessionKey，链路打通。若服务器部署的是旧 jar，需按 `deploy/README.md`「更新部署」重新上传新 jar 并重启。
62: - **账号密码登录**：`YktPasswordLoginApi` 用官方 RSA 公钥加密密码 + 腾讯滑块 ticket/randstr 调 `/pc/login/verify_pwd_login/`；响应 `data:{}` 为空，但 Set-Cookie 直接下发 sessionid（14 天）。
63: - **会话体系**：本地随机 token（`GXH-*`）绑定 sessionid，仅存内存、2 小时有效、滑动续期；所有业务接口经 `X-Session-Token` 解析（兼容 `X-Session-Key`）；支持 `/auth/import-session` 用已知 sessionid 免扫码导入。
64: - **实测**：真实登录拿到学号 55120220 / 55120222，`mock=false`，业务接口正常。
65: 
66: ### 3.2 免扫码进班
67: 
68: 按博客第 1 节：正规在读学生学号已在班级名单，老师点开始上课后 status=1，**用 session_key 作课堂凭据**。`JoinLessonService` 生成官方 `https://changjiang.yuketang.cn/v2/web/join_lesson?lesson_id={id}`（无伪造 sign）。
69: 
70: > **Web 端边界**：官方只认 cookie sessionid，浏览器无法跨域设置；App/小程序 WebView 可注入 cookie 才能真正无感进班。Web 端提供「进入补答」+「官方课堂 ↗」双按钮。
71: 
72: > ⚠️ **2026-11-24 修正（已落地）**：上面这条路线走偏了——「凭据」被直接等同于 sessionid 本身，`join_lesson` 又只认官方域 Cookie，Web 端跨域必 401。真正的课堂凭据是 `checkin` 响应三元组，进班应走官方在读学生固定入口 `/lesson/fullscreen/v3/{lessonId}?source=5`。**现已按形态④后端自渲染实现，见 §6.8**；本段仅保留历史说明。
73: 
74: ### 3.3 随堂超时补答
75: 
76: 老师随堂题随课件下发，倒计时结束但老师未收题时仍可提交、幂等覆盖。`YktInClassQuizApi` 三步：`checkin`(source=21) 取 `Set-Auth` pptJwt → `presentation/fetch` 拿 `slides[].problem` → `problem/answer` 提交（result 按 5 种题型构造）。`InClassQuizService` 负责 jwt 缓存与幂等计数。
77: 
78: > **动态码边界（2026-11-24 16:40 已纠正，见 §7）**：`checkin` 是**签到动作**，不是进课堂的前置。官方在读学生凭 L1 Cookie 打开固定入口 `/lesson/fullscreen/v3/{lessonId}?source=5` 即可建立课堂会话，无 checkin、无动态码。老师开启动态码时 `checkin` 返回 50070 只表示「这个签到动作未被接受」，影响的是签到考勤与自渲染增强的 pptJwt 获取，**不阻断进入课堂会话**。签名动态码不可构造、不绕过。
79: 
80: ### 3.4 课程中心（三级下钻）
81: 
82: `CourseCenter.tsx`：全部课程 → 课时列表 → 课件 PPT + 教师题目。
83: 
84: - **历史课件获取**：关键是用 `activity.courseware_id`（长数字）而非 `activity.id` 作 lesson_id；`YktContentApi.fetchPresentations` 走 `lesson-summary/student?lesson_id={coursewareId}`。
85: - **课件与测验解耦**：所有课件题目抽到顶部「测验区」，可折叠、点击跳转对应页；题目来自 `slides[].problem`（嵌套 `content` 含 body/options/answer），显示参考答案并高亮正确选项——实现"题目预测"。
86: - **PPT 交互**：侧边缩略图预览点击跳转、原生 wheel 监听（passive:false + preventDefault）滚轮翻页不滚动页面、页码输入框回车跳转。
87: 
88: ### 3.5 微信小程序（11-24 新增）
89: 
90: `miniprogram/` 原生小程序作为新客户端连现有 Java 后端，功能对齐 Web 版：
91: 
92: - **页面**：登录（扫码 + sessionid 导入）、当前上课中课程、课程中心三级下钻、课件 PPT + 测验区、随堂补答（选择/填空/主观题）。
93: - **数据层**：`utils/api.js` 封装后端请求，自动带 `X-Session-Token`；401 时用本地 sessionid 调 `/auth/import-session` 无感重导并重放。token/sessionid 存 `wx.setStorageSync`。
94: - **小程序适配**：PPT 翻页用 `swiper` 组件（小程序无 wheel 事件），**支持左右滑动翻页**；布局为**PPT 铺满手机宽度在上、横向缩略图预览条 + 课件切换栏在下**（缩略图随当前页自动滚动），页码输入框；**全屏模式**：右上角按钮（⛶/×）或双击 PPT 切换，全屏时 `position:fixed` 铺满屏幕并隐藏测验区/缩略图/切换栏，翻页控件浮底；**全屏控制栏只留两个按钮**（退出全屏 + 横竖屏切换，用 px 固定尺寸避免横屏时 rpx 变形）：横竖屏切换 `pageOrientation:auto` + `wx.setPageOrientation`，退出全屏/离页自动回竖屏；缩放**仅双指手势**：捏合控制缩放（CSS `transform:translate+scale`，范围 1~4），**双指拖动平移**查看放大后的局部（中点位移），缩放到 1 自动归位；单指滑动仍交 swiper 翻页；翻页只允许左右滑动（全屏）或点缩略图（非全屏），无上一页/下一页栏；官方进班链接无法在小程序直接打开 H5，改为复制到剪贴板并提示在浏览器打开；测试多选项选中态预计算进 data（WXML 不支持函数调用）。
95: - **后端地址**：`app.js` 的 `globalData.apiBase` 默认 `http://localhost:8080/api/v1`，真机需改为局域网 IP 或公网域名。
96: 
97: ### 3.6 其他
98: 
99: - **提前获取题目**：`YktPreviewApi` 从课件页 problem 宽松解析为 Quiz。
100: - **会话续期**：前端静默续签 + 401 无感重导重放。
101: - **隐私**：凭据仅本地 localStorage 与后端内存，不写库；日志脱敏。
102: 
103: ---
104: 
105: ## 4. 抓包方法与接口分析（实测汇总）
106: 
107: > 以下结论来自 2026-11-23~24 真实账号 CDP 抓包与官方前端 JS 逆向，学号已脱敏。
108: 
109: ### 4.1 抓包方法
110: 
111: | 方法           | 步骤 / 工具                                                                                                                                      | 关键注意点                                                                                                                       |
112: | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
113: | 浏览器 CDP      | `--remote-debugging-port=9222 --user-data-dir=<独立profile>` 启动 Chrome，Node.js 连 `webSocketDebuggerUrl`，订阅 `Network.enable` / `Runtime.enable` | ① 连对标签页（登录在官方页）；② Set-Cookie/Set-Auth 要看 `responseReceivedExtraInfo` 原始头；③ React `onWheel` 是 passive，需原生监听才能 preventDefault |
114: | Cookie 读取/注入 | CDP `Runtime.evaluate`（`localStorage.getItem('gxH_sessionId')`）/ `Network.setCookie`                                                         | 调试 profile 与日常 profile 不同；注入后核对页面 user_id，避免沿用旧会话                                                                           |
115: | 官方 JS 逆向     | 下载 `login.js` / `pc.js`，正则搜接口路径与 `checkin`/`credential` 等关键词                                                                                 | API 配置表是接口清单的权威来源                                                                                                           |
116: | 交叉验证         | 开源项目 `powercess/yuketang-mcp`、`Chiu-xaH/Fuck-Yangtze-RainClassroom`、`ChangJiangPPT-Achiever-main`                                            | 以真实账号实测为准                                                                                                                   |
117: 
118: ### 4.2 官方 JS 逆向结果
119: 
120: | 来源                  | 关键发现                                                                                                                                                                  |
121: | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
122: | `login.js`（2.2.682） | 密码登录 `/pc/login/verify_pwd_login/`，1024 位 RSA 公钥（PKCS1Padding），腾讯防水墙 appid `2091064951`；登录成功 `UserID`/`Auth` 存 localStorage，`POST /pc/web_login` 换 sessionid          |
123: | `pc.js`（1.2.363）    | 请求头 `X-Client:web` / `xtbz:ykt` / `X-UID` / `Authorization:Bearer`；接口表含 `get_lesson_token`、`notkn/checkin`（暗号 source=14）、`checkin`、`end_lesson`（UserID+Auth+LessonID） |
124: 
125: ### 4.3 接口协议清单
126: 
127: **登录与会话**
128: 
129: | 接口                                              | 实测结论                                                                             |
130: | ----------------------------------------------- | -------------------------------------------------------------------------------- |
131: | `wss://*/wsapp/`                                | `requestlogin` → 二维码 → `loginsuccess`(UserID/Auth) → `/pc/web_login` 换 sessionid |
132: | `POST /pc/login/verify_pwd_login/`              | 密码 RSA 加密；响应 data 空但 Set-Cookie 下发 sessionid（14 天）                               |
133: | `GET /v2/api/web/userinfo`                      | `school_number` / `user_id` / `name`                                             |
134: | `GET /api/v3/classroom/on-lesson-upcoming-exam` | `onLessonClassrooms[]`：正在上课的 classroomId/courseId/lessonId                       |
135: 
136: **课程与课件**
137: 
138: | 接口                                                                            | 实测结论                                                    |
139: | ----------------------------------------------------------------------------- | ------------------------------------------------------- |
140: | `GET /v2/api/web/courses/list?identity=2`                                     | 课表 `data.list[]`，status 需 live 接口交叉判定                   |
141: | `GET /v2/api/web/logs/learn/{cid}?actype=14&page=&offset=&sort=-1`            | 活动列表；**offset 是每页条数**；type=14 为课堂；用 `courseware_id` 取课件 |
142: | `GET /api/v3/classroom-report/student/lesson-info?lesson_id=`                 | 含 `presentationIds[]`；进行中返回 50030、历史返回 50080            |
143: | `GET /api/v3/lesson-summary/student?lesson_id=`                               | 课件清单 presentations[]                                    |
144: | `GET /api/v3/lesson-summary/student/presentation?presentation_id=&lesson_id=` | `slides[]`：`cover` 图片直链、`problem` 教师题目                  |
145: | `GET /v2/web/join_lesson?lesson_id=`                                          | 官方进班页；只认 cookie sessionid（URL 参数传实测 401）                |
146: | `GET /api/legacy/lesson/get-token?lesson_id=&UserID=`                         | 仍返回 401；`pc.js` 中属 `API.pc.lesson`（教师端/旧版 PC），非学生进班凭据；Bearer 应为 `Set-Auth` pptJwt 而非扫码 Auth（见 §6.3） |
147: 
148: **随堂签到与答题**
149: 
150: | 接口                                                         | 实测结论                                                                             |
151: | ---------------------------------------------------------- | -------------------------------------------------------------------------------- |
152: | `POST /api/v3/lesson/checkin`                              | `{source, lessonId, joinIfNotIn}`；成功响应头 Set-Auth 得 pptJwt；动态码课堂全 source 返回 50070 |
153: | `GET /api/v3/lesson/presentation/fetch?presentation_id=`   | 需 Bearer pptJwt；归档课堂 401 时降级 lesson-summary                                      |
154: | `POST /api/v3/lesson/problem/answer`                       | `{problemId, problemType, dt, result}`，重复提交幂等覆盖                                  |
155: | `POST /api/v3/lesson/notkn/checkin`                        | 暗号进班 source=14，实测 LESSON_UNAUTHORIZED                                            |
156: | `GET /api/v3/lesson/check-in/dynamic-qr-code?c=&t=&s=&v=2` | 动态码二维码，签名 `s` 服务端生成、6~8 秒刷新，无法构造                                                 |
157: 
158: ### 4.4 关键协议细节
159: 
160: 1. **RSA 公钥**：1024 位 PKCS1Padding，Base64；配置在 `password-login.rsa-public-key`。
161: 2. **sessionid 两种来源**：密码登录 Set-Cookie 直接下发；扫码走 wsapp UserID/Auth → web_login 换取。**Auth 不是课堂访问凭据**（Bearer 实测无效，相关代码已回退）；课堂 Bearer 来自 `POST /api/v3/lesson/checkin` 响应头 `Set-Auth`（pptJwt），配套还有 `data.lessonToken`（socketJwt）与 `data.identityId`，详见 §6.2。
162: 3. **历史课件定位**：`activity.courseware_id`（长数字）才是正确 lesson_id；用 `activity.id` 返回 LESSON_DELETED。
163: 4. **problem 结构**：`problem.content` 才是题目正文（body/options/answer/score），顶层为 problemId/sendTime/timeLimit；历史课件题带参考答案。
164: 5. **动态码边界**：官方文档明确"开启动态码时其他进班渠道均无法签到"；这是防代签设计，不应绕过。
165: 6. **Web 进班边界**：官方只认 cookie sessionid，Web 端无法跨域注入；真正无感进班需 App/小程序 WebView。
166: 
167: ---
168: 
169: ## 5. 当前状态与验证
170: 
171: | 功能     | 状态    | 验证                                    |
172: | ------ | ----- | ------------------------------------- |
173: | 扫码登录   | 真实可用  | 学号 55120220/55120222 登录成功，mock=false  |
174: | 账号密码登录 | 真实可用  | 前端登录拿真实学号，CDP 抓包确认 Set-Cookie         |
175: | 免扫码进班  | 按博客实现（**待修正，见 §6**） | `/lessons/join` 返回官方链接 + sessionid 凭据；Web 端跨域 Cookie 问题未解决 |
176: | 随堂补答   | 真实协议  | 接口存在性实测；完整提交需正在上课账号                   |
177: | 课程中心   | 真实数据  | 历史课件 38+28 页、11 页带题（含参考答案）            |
178: | 微信小程序  | 应用卡已创建 | 6 页面 + API 层，原生校验通过，WorkBuddy 预览可用（未发布）  |
179: | 动态码签到  | 不可突破  | 官方防代签设计，穷尽验证后确认                       |
180: 
181: **部署**（`deploy/` 目录）：
182: 
183: - `setup-ip-only.sh`：只有公网 IP 时，Ubuntu 装 Java 21 + systemd 服务，HTTP 直连 8080，小程序勾选"不校验合法域名"后用 `http://IP:8080` 调试
184: - `setup-server.sh`：有备案域名时，加 Nginx 反代 + Let's Encrypt HTTPS，小程序用 `https://域名`
185: - 打包：`cd backend && mvn -q -DskipTests clean package` → jar 在 `target/ykt-backend-0.1.0-SNAPSHOT.jar`
186: - 详细步骤见 `deploy/README.md`
187: - ⚠️ 微信小程序正式使用强制要求 **HTTPS + ICP 备案域名**，纯 IP 仅开发调试可用
188: 
189: ### 3.7 Android APK（11-24 新增）
190: 
191: - **方案**：Capacitor 8 把现有 React Web 前端打包成 Android App（用户选择不改造界面、直接打包 Web 版）。
192: - **后端地址**：构建时 `VITE_API_BASE_URL=http://8.148.83.188:8080/api/v1` 注入；AndroidManifest 加 `usesCleartextTraffic=true` 允许 HTTP。
193: - **环境**：Android SDK 装在 `D/app/AndroidSdk`（cmdline-tools + platform 35/36 + build-tools 35/36，共约 700MB）；Gradle 8.14.3 从腾讯云镜像下载到 `D/app/Gradle`（215MB，20 秒）；`~/.gradle/init.gradle` 配阿里云镜像加速依赖。
194: - **编译**：`cd android && ANDROID_HOME=/d/app/AndroidSdk ./gradlew assembleDebug`；环境变量 `ANDROID_SDK_ROOT` 同指。
195: - **产物**：`releases/gongxiaohu-v1.0.apk`（7.3MB，debug 签名，可直接安装）。
196: - **注意**：Debug APK 用默认调试签名，可安装但不可上架应用商店；正式发布需生成正式签名密钥。
197: - **扫码登录修复（11-24 14:48）**：APK 里扫码后无反应的根因是 Android WebView 会主动 abort 45 秒长轮询连接，而 `beginPoll` 原用 `controller.signal.aborted` 判断是否续轮询——一旦 abort 就静默停止，轮询链断裂。改为用 `pollSession` ref（仅刷新二维码/卸载时递增）控制停止，任何 fetch 错误只要二维码未过期就延迟 500ms 续轮询；倒计时改为按 `expiresAt` 实时计算。
198: - **同步小程序优化到 APK（11-24 14:56）**：Web 前端（CourseCenter.tsx + MakeupApp.tsx）移植小程序课件页优化：全屏按钮+双击切换、CSS 旋转模拟横竖屏、双指缩放+平移（纯手势）、缩略图从左侧移到 PPT 下方横向滚动、精简翻页栏（去掉页码输入）；主页加每分钟自动刷新课程（silent 模式）。APK 重新编译为 7.5MB。
199: 
200: **验证命令**：后端 `mvn test`（27 测试）；前端 `pnpm build`；APK `./gradlew assembleDebug`。
201: 
202: **参考**：官方博客 <https://blog.jiclub.site/2026/yuketang-deep-dive>（本项目受其启发）。
203: 
204: ---
205: 
206: ## 6. session → 课堂凭证 → 免扫码进班：链路推演（2026-11-24）
207: 
208: > 本节为基于官方博客、本地 `pc.js` 逆向、真实账号抓包记录与开源实现的交叉验证得出的方案推演。
209: > **2026-11-24 15:30 已按形态 ④（后端自渲染）落地实现**，见 §6.8。§6.5 为改造清单，§6.7 的抓包项仍需真实开课时逐字段闭环。
210: >
211: > ⚠️ **2026-11-24 16:40 重要纠正（见 §7）**：本节把 `POST /api/v3/lesson/checkin` 当作进入课堂会话的凭证来源，这是错的——checkin 本质是**签到动作**，官方在读学生进课堂不经过它。§6 的三元组凭证链路仍有效，但只是「App 内自渲染」的**可选增强**，不再是进课堂主路径。免动态码进课堂的正确链路见 **§7**。
212: 
213: ### 6.1 结论与概念纠正
214: 
215: 现有实现把「课堂凭证」架空了：`RealYktGateway.exchangeLessonCredential()` 原样返回 sessionid，`JoinLessonService` 再拼一条 `/v2/web/join_lesson?lesson_id=`，既没有「换」，也没有独立凭证；而官方只认本域 Cookie，Web 端跨域必 401。`SignUtil` 自签的 `sign` 官方不认（服务端密钥不可得）。
216: 
217: 还需纠正一处措辞：要换的不是「进入班级」的资格。班级成员资格来自选课名单，在读学生本来就在里面。要换的是**每节课独立、短时效的课堂态会话**。
218: 
219: 三层凭证：
220: 
221: | 层 | 实体 | 获取 | 作用域 / 生命周期 | 存放 |
222: |---|---|---|---|---|
223: | L1 登录态 | `sessionid` + `csrftoken` | `wsapp → /pc/web_login`，或 `/pc/login/verify_pwd_login/` 直接下发 | 全站，14 天 | 本地加密持久化 |
224: | L2 本地会话 | `GXH-*` token | `AuthService` 签发 | 内存，2h 滑动续期 | 后端内存 |
225: | L3 课堂凭证 | `pptJwt` + `socketJwt` + `identityId` | `POST /api/v3/lesson/checkin`（带 L1 + lessonId） | 单节课，下课即废 | 按 lessonId 缓存于内存 |
226: 
227: ### 6.2 完整链路（每步均有依据）
228: 
229: **① 持久化 L1**：`AuthService.importSession()` 当前只存 `sessionid + userId`，应补 `csrftoken`、`X-UID`(user_id)、`university_id/uv_id`；POST 需 `X-CSRFToken` 头。可新增 `/auth/import-cookie` 直接接收整串 Cookie。
230: 
231: **② 定位开课**：`GET /api/v3/classroom/on-lesson-upcoming-exam` → `data.onLessonClassrooms[].lessonId / classroomId / courseId`。课表 `/v2/api/web/courses/list?identity=2` 无 `status` 字段（§4.4、实测记录 §4 均已确认），博客的 `status==1` 是抽象说法。备用 `/api/v3/classroom/on-lesson`（`pc.js` 接口表亦有）。
232: 
233: **③ 换取三元组**：
234: 
235: ```http
236: POST /api/v3/lesson/checkin
237: Cookie: csrftoken=…; sessionid=…
238: X-CSRFToken / X-Client: web / xtbz: ykt / X-UID: …
239: classroom-id / university-id / uv-id: …
240: sessionid: <明文同名头>        ← Android 逆向：缺它即 401 / 50000 UNAUTHENTICATED
241: 
242: {"source":21,"lessonId":"<onLesson[].lessonId>","joinIfNotIn":true}
243: 
244: → Set-Auth: pptJwt              后续课堂 API 的 Authorization: Bearer
245: → data.lessonToken: socketJwt   WebSocket 实时通道
246: → data.identityId               课堂内身份
247: ```
248: 
249: 依据：本仓库 `YktInClassQuizApi` 类注释已列出三元组（但代码只取了 `Set-Auth`）；`pc.js` 全局 axios 拦截器 `if (headers["set-auth"]) window.Authorization = headers["set-auth"]`，请求前统一 `Authorization: "Bearer " + window.Authorization`；开源项目 `check_in_on_listening()` 同时读取这四个值。
250: 
251: **④ 进课堂页**：
252: 
253: | 端 | 入口 | 说明 |
254: |---|---|---|
255: | PC | `/lesson/fullscreen/v3/{lessonId}?source=5` | `pc.js` 的 `openFullscreen()`，首页点「正在上课」即此跳转 |
256: | H5 / WebView | `/m/v2/lesson/student/{lessonId}` | 开源项目实测课堂页 |
257: 
258: 两者**都没有 sign、没有 ts**——这正是替代动态二维码临时进班链接的关键：走官方给在读学生的固定入口。`join_lesson?lesson_id=&sign=&ts=` 是给旁听生的临时链接，服务端签名，客户端无法构造也不必构造。
259: 
260: ### 6.3 三个关键辨析
261: 
262: 1. **`get-token` 不是学生进班凭据**：`/api/legacy/lesson/get-token` 在 `pc.js` 的 `API.pc.lesson` 表中，同表相邻的是 `ppt_upload_perm`、`problem_results_pc` 等教师端/旧版 PC 接口。§4.3 实测 401 的根因更可能是缺全套会话头（csrftoken / X-CSRFToken / X-Client / xtbz / X-UID / classroom-id / university-id / uv-id / 明文 sessionid 同名头）且 Bearer 用错——Bearer 必须是 `Set-Auth` 的 pptJwt，不是微信登录 Auth。不建议继续在这条路上投入。
263: 2. **`source` 有两套语义，勿混**：checkin 的 `source`：`21` 二维码、`23` APP、`5` 公众号、`22` 暗号（开源项目配置表）；`pc.js` 里 `?source=5|14` 是**页面跳转参数**；`POST /api/v3/lesson/notkn/checkin` 的 `{source, inviteCode}` 是**暗号加入班级**（§4.3 实测 LESSON_UNAUTHORIZED 是 payload 用错，不是接口废弃）。
264: 3. **不要自签 sign/ts**：`join_lesson` 的 sign 由服务端密钥签发。现有 `join-signature.secret` + `SignUtil` 只能作本地幂等/防误触用途，不应在文档或注释中称「服务端校验」。
265: 
266: ### 6.4 跨域 Cookie 是真正的落地门槛
267: 
268: 课堂页只认官方域 Cookie，`localhost:3000` 无法注入。「换到凭证」≠「页面能进」。四种形态：
269: 
270: | 形态 | 做法 | 适用 |
271: |---|---|---|
272: | ① 同源反向代理 | Spring Boot `/ykt/**` 代理 HTML/JS/API/WS，改写 `Set-Cookie` Domain/Path、重写绝对资源 URL、透传 `X-CSRFToken` | 本地 Web 最完整 |
273: | ② CDP / 扩展注入 | `Network.setCookie` 或扩展 cookies API 写官方域，再 `window.open` | 个人单机最轻（本仓库抓包已用过 CDP） |
274: | ③ WebView 注入 | `CookieManager` 预置后加载课堂页 | App / 小程序（博客路线）；当前小程序 §3.5 为剪贴板降级，未做注入 |
275: | ④ 后端自渲染 | 持 pptJwt 直接调 `presentation/fetch`、`problem/answer`，前端自绘课件与题目 | 最快验证、隐私最好 |
276: 
277: 本项目建议先做 ④（现有 `YktInClassQuizApi` + `YktContentApi` 即雏形），再视需要做 ①。
278: 
279: ### 6.5 代码改造清单
280: 
281: | 位置 | 现状 | 改法 |
282: |---|---|---|
283: | `RealYktGateway.exchangeLessonCredential()` | 原样返回 sessionid | 改为 `enterLesson(session, lessonId)` → 凭证 record |
284: | `YktInClassQuizApi.checkinForJwt()` | 只取 `Set-Auth`，丢 `lessonToken/identityId`；缺全套 header | 抽为通用 `LessonCredentialClient`，补齐头部 |
285: | `JoinLessonService.join()` | 拼 `join_lesson?lesson_id=` + 自签 sign | 返回 `fullscreen/v3` URL + 凭证状态 |
286: | `Models.JoinLessonResult.credential`、`api.ts:joinLesson()`、`MakeupApp:openOfficialClassroom()` | 凭证回传前端却未使用，还把登录态暴露给浏览器 | 凭证只留后端内存，前端只拿 `entryId` |
287: | `application.yml:join-signature` | 语义误导 | 降级为本地防误触配置 |
288: 
289: ### 6.6 失败矩阵与边界
290: 
291: | 现象 | 处理 |
292: |---|---|
293: | `401` / `50000 UNAUTHENTICATED` | 补齐全套会话头；仍失败则重新 `import-session` / 扫码 |
294: | `50070 DYNAMIC_QR_CHECK_IN_REFUSED` | 老师开启动态码签到，**停止程序化签到**，提示本人当场扫码；课堂页仍可用 `source=5` 打开，看题降级 `lesson-summary` |
295: | `50030` / `50080` | lessonId 类型错：进行中用 on-lesson 的 lessonId，历史用 `activity.courseware_id` |
296: | `50004 LESSON_END` | 课堂结束，清缓存 |
297: | `50019` | 未做学校身份绑定，走绑定流程 |
298: 
299: **边界**：本节只替代「进班临时链接」，不触碰签到动态码（`/api/v3/lesson/check-in/dynamic-qr-code?c&t&s&v=2`，6~8 秒刷新、服务端签名，见 §4.4 第 5 条）。进班由用户主动触发，不做无人值守自动签到。凭据仅内存 + 本地加密存储，日志脱敏。
300: 
301: ### 6.7 待补抓包 → 已由后端诊断自动覆盖（2026-11-24 15:55）
302: 
303: 原先这 5 项需要用户手工抓包，现已通过**后端验证诊断**自动结构化返回，见 §6.9：
304: 
305: 1. 首页点「正在上课」→ `fullscreen/v3` 的完整 Network —— 仍需浏览器抓包（后端不代理官方页）；
306: 2. `checkin` 全套 request headers —— ✅ **自动返回**（含哪些头有值 / `missingHeaders`）；
307: 3. 响应 `Set-Auth` / `lessonToken` / `identityId` / `code` —— ✅ **自动返回**（存在性 + 不可逆指纹，不含明文）；
308: 4. WebSocket URL 中 socketJwt / identityId 的用法 —— 部分：socketJwt 存在性已记录，URL 拼法仍需抓包；
309: 5. `get-token` 归属 —— ✅ 已静态确认属 `pc.js` 教师端接口表，非学生进班凭据，无需抓包。
310: 
311: **怎么用**：真实开课时点「进入课堂」触发 checkin → 在课堂页展开「验证诊断 · checkin 证据」→ 即可看到每条尝试的头部清单、缺失头、HTTP/业务码、Set-Auth 指纹、三元组齐全情况与耗时；也可 `GET /api/v1/diagnostics`（需登录）。`DELETE /api/v1/diagnostics` 清空重测。
312: 
313: **依据**：官方博客 §1；本地 `backend/pc.js` 接口表与 axios 拦截器逆向；`长江雨课堂-真实登录与抓包实测.md`；`雨课堂扫码协议-调研记录.md`；开源实现 `ironhxs/Fuck-Yangtze-RainClassroom`（source 取值与三元组）、`2422725977/yuketang-reverse`（明文 sessionid 头与两套鉴权链）。
314: 
315: ### 6.8 形态 ④ 已落地实现（2026-11-24 15:30）
316: 
317: 按 §6.5 清单逐项实现，**后端自渲染、不进官方页、不下发任何凭证**。29 个测试全通过。
318: 
319: | 改造 | 实现 |
320: |---|---|
321: | ① L1 完整化 | `AuthModels.SessionContext` 记录 sessionid/csrftoken/X-UID/university-id/uv-id/classroom-id/rawCookie；`AuthService.importCookie()` + `POST /auth/import-cookie` 收整串 Cookie；**扫码登录链路** `YktWebSocketLogin.exchangeSessionId()` 改为返回完整上下文（从 `user_info` 的 Cookie 带出 csrftoken 并解析 university_id/uv_id），`AuthCredential` 携带 context 一路存入 L2 会话 |
322: | ② 凭证客户端 | 新增 `gateway/LessonCredentialClient`：`POST /api/v3/lesson/checkin` 带全套头（Cookie / sessionid 同名头 / X-CSRFToken / X-Client / xtbz / X-UID / classroom-id / university-id / uv-id），解析 `Set-Auth` pptJwt + `data.lessonToken` socketJwt + `data.identityId`；按 sessionId#lessonId 内存缓存 2h；source 21→23→5→22 回退 |
323: | ③ 失败矩阵 | 401/50000 提示重新导入完整 Cookie；**50070 动态码签到停止程序化签到**（抛出可识别异常，上层降级看题，绝不绕过）；50004 清缓存；50030/50080 lessonId 类型提示；50019 身份绑定 |
324: | ④ 进班链路 | `RealYktGateway.exchangeLessonCredential()` 不再原样返回 sessionid，改为委托凭证客户端换 pptJwt；`JoinLessonService.join()` 返回 `fullscreen/v3/{lessonId}?source=5` 固定入口 + `entryId` + 凭证状态，`join_lesson?lesson_id=` 与自签 sign 全部移除 |
325: | ⑤ 凭证不出后端 | `JoinLessonResult` 删除 `credential` 字段；新增 `entryId / credentialReady / hint`；`ClassSession`（含凭证）只存内存 Map，前端凭 entryId 调用 |
326: | ⑥ 自渲染接口 | `GET /api/v1/lessons/{entryId}/render` 返回课件（`YktContentApi`，lesson-summary 路径）+ 题目（`InClassQuizService` 复用已换出的 pptJwt，跳过二次 checkin）+ 凭证状态/hint |
327: | ⑦ 前端自绘 | `api.ts` 新增 `fetchClassroomRender` / `importCookie`；`MakeupApp` 主页「进入课堂」按钮改为先 join 再 render，新增 `SelfRenderedClassroom` 组件自绘 PPT（课件切换 + 缩略图）与题目；官方页降级为「在浏览器打开」次要选项；`LoginView` 新增「Cookie」登录 Tab（`import-cookie`），供账密/已有浏览器会话用户手动补齐 L1 |
328: | ⑧ 安全联动 | 登出时 `AuthController.logout()` 同时清除 `LessonCredentialClient` 凭证缓存、`JoinLessonService` 自渲染会话、`InClassQuizService` jwt 缓存 |
329: | ⑨ 配置语义 | `application.yml:join-signature` 注释降级为「本地防误触 / 幂等 + entryId TTL」，明确官方不认此 sign |
330: 
331: **边界保持**：仅替代进班临时链接，不触碰签到动态码；进班由用户主动点击触发；凭据仅内存，日志脱敏；`verify-sign` 仅本地防误触。
332: 
333: **仍需真实开课验证**（§6.7 抓包）：checkin 全套头是否齐全、三元组字段是否如期返回、socketJwt/identityId 的实际用途。当前实现在字段缺失时会给出明确降级提示，不会静默失败。
334: 
335: ### 6.9 验证诊断日志（2026-11-24 15:55 新增）
336: 
337: 为闭环 §6.7 的验证项，新增**结构化、脱敏、可查询**的诊断能力，无需用户手工抓包。
338: 
339: | 组件 | 说明 |
340: |---|---|
341: | `gateway/LessonDiagnostic` | 内存环形缓冲（最近 50 条），`Record` 记录每次 checkin：时间/lessonId/source/URL、**全套请求头有无**、`missingHeaders`、HTTP 状态、业务码与消息、`Set-Auth` 存在性+指纹、socketJwt/identityId 有无、结果分类、耗时；`Checklist` 对照 §6.7 五项给出达成状态与下一步提示 |
342: | `LessonCredentialClient.checkin()` | `finally` 中无论成功失败都落一条诊断；`outcome` 分类 SUCCESS / AUTH_EXPIRED / DYNAMIC_QR / LESSON_END / BAD_LESSON_ID / NEED_BIND / NO_SET_AUTH / NETWORK_ERROR |
343: | `DiagnosticsController` | `GET /api/v1/diagnostics` 返回报告（需有效 `X-Session-Token`）、`DELETE` 清空 |
344: | 前端 | `api.ts` 加 `fetchDiagnostics` / `clearDiagnostics`；`SelfRenderedClassroom` 加「验证诊断 · checkin 证据」折叠面板，展示 ①~⑤ 清单状态与逐条记录 |
345: | `application.yml` | **修正 logging 包名**（`site.jiclub.gongxiaohu` → `com.jiclub.gongxiaohu`，此前 debug 级别从未生效）；控制台新增脱敏摘要日志 `[诊断] checkin ...` |
346: 
347: **隐私边界**：只记字段名、「有值/缺失」、长度与 4 位掩码前缀；**绝不记录 sessionid / csrftoken / pptJwt / socketJwt / identityId 明文**，不落盘、不写日志文件，进程重启即失效。`DiagnosticsTest` 专门断言响应不含任何敏感明文（33 个测试全通过）。
348: 
349: ---
350: 
351: ## 7. 认知纠正：不经动态码进入课堂会话（2026-11-24 16:40）
352: 
353: > 本节推翻 §6 的一处根本性误判。用户指出「博客分享的正是免动态码进课堂的思路」，重新核对博客原文、官方帮助文档与 `pc.js` 后确认：**之前把 `checkin` 当成进课堂的必经步骤，是错的。**
354: 
355: ### 7.1 错在哪
356: 
357: | 之前的判断 | 事实 |
358: |---|---|
359: | `POST /api/v3/lesson/checkin` 是换取「课堂态凭证」的唯一入口，进课堂必须先过它 | `checkin` 字面即**签到动作**。官方 `pc.js` 首页点「正在上课」时 `openFullscreen()` 直接 `window.open("/lesson/fullscreen/v3/" + lessonId + "?source=5")`，**无 checkin、无 sign、无 ts、无动态码** |
360: | 动态码开启（50070）就进不了课堂 | 博客原文：「这种扫码机制本质上是给**旁听生**使用的」；官方帮助列出 5 种进课堂方式，「正在上课」提示是**已加入班级学生的独立通道**。动态码约束的是**签到考勤**，不是建立课堂会话 |
361: | 50070 = `DYNAMIC_QR_CHECK_IN_REFUSED` 专用码 | `pc.js` 中 50070 对应 `lessonEndMsg`「老师已经下课了，不能加入课堂」，属 `notkn/checkin`（暗号加班级）分支；它是「该签到/加入动作未被接受」的通用码，不能单因归因 |
362: | `joinIfNotIn` 是官方参数 | 在 `pc.js` 中出现 **0 次**，源自开源自动签到脚本，不是官方在读学生通道参数 |
363: | 必须拿 `pptJwt` 才能看课件 | `lesson-summary/student` 系列只需 L1 sessionid；`pptJwt` 仅 `presentation/fetch` 等课堂 API 需要 |
364: 
365: **根因**：为了自渲染拿 `pptJwt`，主动调用了旁听生的签到通道，等于自己给自己设了动态码门槛。方案 ④ 整体架在这个错误前提上。
366: 
367: ### 7.2 正确链路（对齐博客 §1）
368: 
369: ```
370: ① L1 登录态：sessionid + csrftoken（扫码 web_login / 账密 / 整串 Cookie 导入）
371: ② 开课发现：GET /api/v3/classroom/on-lesson-upcoming-exam → onLessonClassrooms[].lessonId
372: ③ 进入课堂会话：官方在读学生固定入口 /lesson/fullscreen/v3/{lessonId}?source=5
373:    （同源 Cookie；PC / H5 均可；无 sign、无 ts、无 checkin、无动态码）
374: ④ 可选增强：仅当需要在 App 内自绘课件/题目时，才按需尝试 checkin 换 pptJwt
375: ```
376: 
377: 班级成员资格来自选课名单，**不需要任何凭证去换**；动态码只是旁听生的临时进班链接（`join_lesson?lesson_id=&sign=&ts=`），与在读学生通道并列而非前置。
378: 
379: ### 7.3 真正的门槛是跨域 Cookie，不是动态码
380: 
381: 官方课堂页只认 `changjiang.yuketang.cn` 域下的 Cookie。三种落地形态（按端选择）：
382: 
383: | 形态 | 做法 | 适用 |
384: |---|---|---|
385: | **A 原生 WebView 注入** | Android `CookieManager.setCookie(官方域, ...)` + **App 内 WebView** 打开固定入口 | APK（本次已实现） |
386: | B 同源反向代理 | Spring Boot `/ykt/**` 代理 HTML/JS/API/WS，改写 `Set-Cookie` Domain、重写绝对资源 URL | Web 端最完整 |
387: | C CDP / 扩展注入 | `Network.setCookie` 写官方域后 `window.open` | 个人单机调试 |
388: 
389: > 关键坑：CookieManager 注入的 Cookie **只对 App 内 WebView 生效**，系统浏览器 / Chrome Custom Tab 不共享，因此必须用 `ClassroomWebViewActivity` 承载官方课堂页。
390: 
391: ### 7.4 本次落地改动
392: 
393: | 位置 | 改动 |
394: |---|---|
395: | `JoinLessonService` | `join()` 默认**不再调用 checkin**；新增 `needCredential` 开关，仅自渲染增强时才尝试换 pptJwt，失败不阻断进课堂 |
396: | `LessonController` | `POST /lessons/join?enhance=false`（默认不签到）；新增 `GET /lessons/entry-cookie` 返回注入用 L1 Cookie（需登录） |
397: | `LessonCredentialClient` | 类注释与 50070 分支文案修正：checkin 是签到动作、非进课堂前置；`outcome` 由 `DYNAMIC_QR` 改为 `CHECKIN_REFUSED` |
398: | Android 原生 | 新增 `ClassroomEntryPlugin`（Cookie 注入 + 启动 WebView）、`ClassroomWebViewActivity`（JS/DOM/桌面 UA/第三方 Cookie），`MainActivity` 注册插件，Manifest 声明 Activity |
399: | 前端 | `classroomEntry.ts` 用 `registerPlugin` 封装原生调用；`api.ts` 加 `fetchEntryCookie` / `joinLesson(enhance)`；`MakeupApp` 主页「进入课堂」优先走固定入口 + 注入，失败降级自渲染 |
400: | 旧方案④ | 保留为**可选自渲染增强**，不再是进课堂主路径 |
401: 
402: **边界不变**：动态码签到（考勤）仍需本人当场扫码，本项目不做、不尝试绕过；进课堂由用户主动点击触发；Cookie 仅内存 + 本地，日志脱敏。
403: 
404: ### 7.5 待真实开课验证
405: 
406: 1. 注入 Cookie 后 `fullscreen/v3` 是否正常建立 WebSocket 会话（桌面 UA 是否被官方页接受）；
407: 2. 进行中课堂能否直接看到实时课件（不签到时 `lesson-summary` 曾实测返回 `errorCode:-1`，需确认固定入口会话建立后是否解锁）；
408: 3. H5 入口 `/m/v2/lesson/student/{lessonId}` 是否比 PC 页更适合移动端 WebView；
409: 4. 官方页是否会二次跳转 `join_lesson` 或要求动态码（若有，说明该课堂额外开启了限制）。
410: 
411: 诊断面板（§6.9）已能记录 checkin 全套证据；建议下次开课先清空诊断，分别点「原生进入」与「自渲染增强」各一次，对比结果即可闭环。
412: 
413: ---
414: 
415: ## 8. 用户选定：后端自渲染为默认方案（2026-11-24 16:50）
416: 
417: 用户在 §7 的三条候选中选择 **形态 C「后端自渲染」** 作为默认主路径。
418: 
419: ### 8.1 与 §6 旧方案④的本质区别
420: 
421: | | §6 旧方案④ | §8 当前方案 |
422: |---|---|---|
423: | 课件/题目来源 | `checkin` 换 pptJwt → `presentation/fetch` | 仅凭 L1 sessionid → `lesson-summary` 系列 |
424: | 是否调用 checkin | **是（必经）** | **默认完全不调用** |
425: | 是否受动态码影响 | 是（50070 阻断） | **否** |
426: | 失败时 | 降级到 lesson-summary | 本身就是 lesson-summary，无降级 |
427: | 提交答案 | 需 pptJwt | 仍需本人扫码签到（唯一保留的边界） |
428: 
429: 旧方案④把 `checkin` 当前置，是 §7 已纠正的认知错误；当前方案彻底去掉它。
430: 
431: ### 8.2 落地改动
432: 
433: - `InClassQuizService.listQuizzesForSelfRender()`：新增公开无签到取题入口，直接走 `fetchFromContent`（lesson-summary），跳过 `ensureJwt` → 不再触发 checkin
434: - `LessonController.render()`：默认纯自渲染；`?enhance=true` 且凭证就绪时才可选实时题增强，失败自动回落
435: - `MakeupApp.openOfficialClassroom()`：默认 `join(false)` + `render`，不再优先原生 Cookie 注入
436: - 自渲染课堂页保留两个**可选**入口：App 内 WebView 注入官方域 Cookie、浏览器打开官方固定入口
437: - 诊断面板文案说明「自渲染不调用 checkin，无记录属正常」
438: - `deploy/README.md` 重写为当前方案的完整部署指南（后端 jar / APK / Web / 小程序四端 + 自检清单）
439: 
440: ### 8.3 已知限制
441: 
442: - 进行中课堂的 `lesson-summary` 可能尚未归档生成 → 课件/题目可能为空；历史课时稳定可用。此点需真实开课验证（§7.5）
443: - 提交随堂答案仍需本人在雨课堂扫码签到，这是官方防代签设计，不做绕过
444: - Web 端无法注入官方域 Cookie，因此主路径选后端自渲染是正确取舍；官方页按钮仅在浏览器已登录雨课堂时有效
445: 
446: ---
447: 
448: ## 9. 服务日志与行为审计（2026-11-24 17:55 新增）
449: 
450: ### 9.1 为什么改
451: 
452: 改造前的问题（逐条实测）：
453: 
454: | 问题 | 影响 |
455: |---|---|
456: | `AuthService.maskUserId()` 把学号打成 `20****23` | **无法回答"哪个学号登录了"**，只有前 2 后 2 位 |
457: | 没有文件输出，只靠 systemd `StandardOutput=append:app.log` | 单文件无限增长，无轮转、无压缩、无保留期 |
458: | 日志 pattern 只有 `HH:mm:ss.SSS`，**没有日期** | 跨天排查无法定位，重启后无法分辨先后 |
459: | 没有访问日志 | 不知道服务有没有被访问、哪个接口慢/报错、谁在用 |
460: | 启动/崩溃输出与业务日志混在一个文件 | 排查困难 |
461: 
462: ### 9.2 现在的结构
463: 
464: | 日志 | 位置 | 内容 | 学号 | 轮转/保留 |
465: |---|---|---|---|---|
466: | 运行日志 | `logs/app.log` | 业务链路、错误堆栈、每请求一行访问记录 | **脱敏** `20****23` | 按天 + 50MB，保留 14 天，gzip |
467: | 审计日志 | `logs/audit.log` | 登录/登出/进课堂事件 | **明文** | 按天 + 20MB，保留 180 天，gzip |
468: | 启动期日志 | `journalctl -u ykt-backend` | logback 初始化前/进程崩溃输出 | — | journald 自管 |
469: 
470: 配置：`src/main/resources/logback-spring.xml`，目录由环境变量 `LOG_DIR` 指定（systemd 中为
471: `/opt/ykt-backend/logs`），未设置时落到运行目录 `logs/`。
472: 
473: 审计事件（logger 名 `AUDIT`，独立文件、不混进 app.log）：
474: 
475: ```
476: 2026-11-24 18:01:02.345 INFO  [tomcat-handler-3] AUDIT - LOGIN_OK method=qrcode userId=2023012345 ip=1.2.3.4 client=AndroidApp(okhttp)
477: 2026-11-24 18:05:11.002 INFO  [...] AUDIT - LOGIN_OK method=password userId=2023012345 ip=1.2.3.4 client=-
478: 2026-11-24 18:05:30.117 INFO  [...] AUDIT - LOGIN_OK method=import-cookie userId=2023012345 ip=1.2.3.4 client=-
479: 2026-11-24 18:09:44.881 INFO  [...] AUDIT - ENTER_LESSON userId=2023012345 courseId=123 lessonId=456 entryId=... credential=fallback ip=... client=...
480: 2026-11-24 18:20:03.442 INFO  [...] AUDIT - LOGOUT userId=2023012345 ip=1.2.3.4 client=-
481: 2026-11-24 18:21:10.005 INFO  [...] AUDIT - LOGIN_FAIL method=password account=2023**** reason=密码错误 ip=...
482: ```
483: 
484: 访问日志（logger 名 `ACCESS`，写入 app.log）：
485: 
486: ```
487: 2026-11-24 18:01:02.345 INFO  [tomcat-handler-3] ACCESS - ip=1.2.3.4 GET /api/v1/courses status=200 耗时=83ms user=20****45
488: ```
489: 
490: `method` 取值对应 4 种登录入口：`qrcode`（扫码）/ `password`（账密）/ `import-session`（sessionid 导入）/ `import-cookie`（整串 Cookie 导入）。
491: 
492: ### 9.3 隐私边界
493: 
494: - **记录**：明文学号（仅 audit.log，文件权限 600）、客户端 IP、渠道、课程/课时 ID、结果
495: - **不记录**：token、Cookie、sessionid、csrftoken、pptJwt、socketJwt、identityId、密码
496:   —— `LoggingAuditTest` 断言两份日志均不含 token 原文与 Cookie 原文
497: - app.log 中学号一律脱敏，给排障用；需要定位到具体人时查 audit.log
498: 
499: ### 9.4 代码位置
500: 
501: | 文件 | 作用 |
502: |---|---|
503: | `resources/logback-spring.xml` | 三路 appender + 独立 `AUDIT` logger |
504: | `common/AuditLog.java` | 审计事件封装（LOGIN_OK/LOGIN_FAIL/LOGOUT/ENTER_LESSON） |
505: | `common/AccessLogFilter.java` | `/api/**` 每请求一行，`X-Forwarded-For` 取真实 IP |
506: | `common/RequestContext.java` | 请求线程上下文（IP/方法/路径/UA），不放凭证 |
507: | `service/AuthService.peekUserId()` | 仅供日志反查学号，不续期、不抛异常 |
508: | `deploy/logs.sh` | 服务器端日志速查/打包脚本 |
509: | `LoggingAuditTest` | 2 项验收：明文学号 + 访问行存在 + 无凭证泄漏 |
510: 
511: ### 9.5 获取日志（不需要后端接口）
512: 
513: 服务器上直接读文件，或打包回传：
514: 
515: ```bash
516: # 最近 20 次登录（回答"哪个学号登录了"）
517: grep LOGIN_OK /opt/ykt-backend/logs/audit.log | tail -20
518: 
519: # 某学号的全部行为
520: grep 'userId=2023012345' /opt/ykt-backend/logs/audit.log
521: 
522: # 实时看运行日志
523: tail -f /opt/ykt-backend/logs/app.log
524: 
525: # 只看错误
526: grep -E ' (ERROR|WARN) ' /opt/ykt-backend/logs/app.log | tail -50
527: 
528: # 打包下载
529: tar -czf /tmp/ykt-logs.tar.gz -C /opt/ykt-backend/logs .
530: ```
531: 
532: 本机拉取（Windows 命令行 / Git Bash 均可）：
533: 
534: ```
535: scp root@8.148.83.188:/tmp/ykt-logs.tar.gz .
536: scp root@8.148.83.188:/opt/ykt-backend/logs/audit.log .
537: ```
538: 
539: `.` 表示**执行命令时所在的当前目录**。想固定归档位置，用 `bash deploy/fetch-logs.sh`，
540: 自动落到 `logs-backup/<时间戳>/`（已在 `.gitignore` 中忽略），或显式指定目标路径。
541: 
542: 拉回来的内容：整目录版含 `app.log` + `audit.log` + 历史轮转 `*.log.gz`；`ONLY=audit` 只含 audit；
543: 直接 `scp audit.log` 只含当前文件。三者都只是文本日志，不含 jar/配置/Cookie。
544: 
545: 解析（`deploy/parse-logs.sh`，支持 `.gz` 与 stdin）：
546: 
547: ```bash
548: bash deploy/parse-logs.sh logs-backup/xxx/audit.log users            # 学号维度：次数/首次/末次/渠道/IP
549: bash deploy/parse-logs.sh logs-backup/xxx/audit.log user 2023012345  # 某学号时间线
550: bash deploy/parse-logs.sh logs-backup/xxx/audit.log events           # 事件明细
551: bash deploy/parse-logs.sh logs-backup/xxx/app.log  access            # 接口：次数/错误/耗时/用户数
552: ```
553: 
554: 或直接用服务器脚本：`bash /opt/ykt-backend/logs.sh login 20` / `user <学号>` / `pack`。
555: 
556: ---
557: 
558: ## 10. 扫码后停留二维码页：同类故障二次修复（2026-11-25）
559: 
560: ### 10.1 沿用 11-24 的经验
561: 
562: 11-24 的根因：`YktWebSocketLogin.handleMessage` 让不含 Auth 的 `subscribe_status`
563: 提前 `complete(loginFuture)`，真正的 `loginsuccess` 再 complete 无效 → 扫码成功却永远登不上。
564: 修复原则是：**状态通知不能消费一次性登录凭据，必须以关键字段（Auth）为准**。
565: 
566: ### 10.2 这次发现的同类单点故障
567: 
568: 11-24 的修复只保证"消息不被误消费"，但 `pollAuthStatus` 仍把
569: 「等待扫码消息」和「用 UserID+Auth 换 sessionid」绑在一次 poll 里：
570: 
571: - 扫码确认已到达、`loginFuture` 已完成；
572: - 若 `user_info` 超时 / `web_login` 异常 / 网络抖动导致 `exchangeSessionId` 抛异常，
573:   原代码 `catch (Exception e) { cleanup(...); }` 会把 flow 连同 WebSocket 一起清掉；
574: - 后续 poll 拿到的是 `flow == null` → 一直 409 → 前端持续重试到二维码过期。
575:   用户看到的就是**扫了码、手机已确认，但二维码页不动**。
576: 
577: 这与 11-24 属于同一类问题：一次性获取到的扫码凭据被后续失败"顺手丢掉"。
578: 
579: ### 10.3 后端修复
580: 
581: | 改动 | 说明 |
582: |---|---|
583: | `LoginFlow.credential` | 收到带 Auth 的 `loginsuccess` 立即缓存原始消息；`loginFuture` 只负责唤醒等待者 |
584: | `LoginFlow.cacheCredential()` | 包可见；校验 Auth 非空才接受；迟到/重复消息不覆盖首次凭据 |
585: | `pollAuthStatus()` | 已有缓存凭据时**跳过 45 秒等待**直接换 sessionid；不受二维码有效期限制（扫码已确认） |
586: | 失败处理 | 换 sessionid 异常只退避 3 秒并保留 flow/凭据，下次 poll 直接重试；绝不 `cleanup()` |
587: | `handleMessage()` | 触发条件放宽到 `op=loginsuccess` 或消息含 `Auth/auth`（兼容 op 缺失的推送） |
588: | `AuthController.poll()` | 成功打 INFO（ticket 指纹+学号）、未确认打 DEBUG，日志可区分"消息没到"与"消息到了但换取失败" |
589: 
590: 新增 `YktWebSocketLoginFlowTest`（2 项）回归保护：无 Auth 通知不消费凭据、带 Auth 成功消息不被覆盖/丢弃。
591: 
592: ### 10.4 前端修复（APK / Web 共用 `src/makeup`）
593: 
594: 沿用小程序 `loginSuccess` 的兜底思路：
595: 
596: - `pollLogin` 返回后先校验 `token` 与 `userId`，缺失则当失败续轮询，不带 undefined 进主界面；
597: - 成功凭据存进 `successResult` ref，800ms 自动跳转**不再被 `session` 递增拦截**
598:   （用户刚点过"重新获取二维码"但手机其实已确认时，旧代码会丢掉这次成功）；
599: - 二维码页成功态新增 **「进入首页」** 按钮，自动跳转被 WebView/环境拦截时手动进；
600: - 连续 3 次轮询失败显示提示：若手机已确认，去后端日志查「收到登录成功凭据」。
601: 
602: ### 10.5 部署与验证
603: 
604: **两端都要更新**（这个问题同时涉及后端凭据缓存和前端跳转兜底）：
605: 
606: ```bash
607: # 后端
608: scp releases/ykt-backend-scheme4.jar root@<IP>:/opt/ykt-backend/ykt-backend.jar
609: ssh root@<IP> "systemctl restart ykt-backend"
610: 
611: # APK（前端已打包进 assets）
612: 把 releases/gongxiaohu-v1.0.apk 传到手机覆盖安装
613: ```
614: 
615: 验证时看后端日志：
616: 
617: - 只看到 `扫码轮询暂未拿到凭据` → 消息没到（二维码/WS 问题）；
618: - 出现 `收到登录成功凭据` 但随后 `换取 sessionid 失败` → 新逻辑会保留凭据，用户无需重新扫码，

(Output capped. Showing lines 1-618. Use offset=619 to continue.)
</content>

[assistant]
Continue reading 技术说明.md from 619, plus the other docs.

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\技术说明.md","offset":619}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\正确实现链路.md"}

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\技术说明.md</path>
<type>file</type>
<content>
619:   前端下一次轮询即重试；
620: - 出现 `真实扫码登录成功` → 应立刻跳转，若没有则点二维码页的「进入首页」。
621: 
622: ---
623: 
624: ## 11. 扫码轮询改短轮询 + 结果幂等（2026-11-25 09:10 定案）
625: 
626: ### 11.1 这次的证据链
627: 
628: 用户提供两段服务端日志：
629: 
630: ```
631: 2026-11-25 09:00:24.348 AUDIT - LOGIN_OK method=qrcode userId=55120220 ...
632: 2026-11-25 09:00:37.493 ACCESS - POST /api/v1/auth/poll status=200 耗时=4ms user=anonymous
633: 2026-11-25 09:01:39.649 ACCESS - POST /api/v1/auth/poll status=200 耗时=3ms user=anonymous
634: ```
635: 
636: 三个关键读数：
637: 
638: | 读数 | 含义 |
639: |---|---|
640: | `LOGIN_OK` | 扫码确认、换 sessionid、签发 token 全部成功，后端没有故障 |
641: | `status=200` 但 `耗时=4ms` | **不是成功**。`BizException` 被 `GlobalExceptionHandler` 包成 HTTP 200 + body `code=409`；4ms 说明网关里已经 `flows.get(ticket) == null`——登录流程早已被 `cleanup()` 掉 |
642: | 4ms 而不是 45s | 若流程还在、只是没确认，原实现会 `loginFuture.get(45s)` 阻塞约 45 秒。秒回只可能是"流程不存在" |
643: 
644: 结论：**token 已签发，但那次成功响应没被前端消费掉**；流程随即清理，同一 ticket 之后的轮询全部是
645: "尚未确认"，用户只能重扫。这与上一轮 `subscribe_status` 提前完成 future 属于同一族问题——
646: 单点、一次性、丢了就没有第二次机会。
647: 
648: ### 11.2 两个真实成因
649: 
650: 1. **45 秒长轮询在移动端活不下来。** Android WebView 与运营商 NAT 会把空闲 30–40 秒的连接静默掐断。
651:    服务端在扫码瞬间把响应写回这条已死连接，客户端只会得到网络错误并重试；而此时流程已清理。
652: 2. **结果只发一次。** 成功后立刻 `cleanup()`，同一个 ticket 不再有任何取回登录态的途径。
653: 
654: ### 11.3 改法
655: 
656: | 层 | 改动 |
657: |---|---|
658: | `YktGateway` / `YktWebSocketLogin` / `MockYktGateway` | `pollAuthStatus(ticket, waitSeconds)`：`waitSeconds<=0` 立即返回，不阻塞；原单参方法保留（默认 45 秒），小程序等旧客户端行为不变 |
659: | `AuthService` | 新增 ticket → `LoginResult` 的幂等缓存（TTL 10 分钟、上限 512 条，仅内存）。同一 ticket 再轮询直接返回**同一个 token**，不重新签发 |
660: | `AuthController` | `POST /auth/poll` 接受 `{ticket, wait}`；`wait` 缺省 45（兼容），前端传 `0`。成功日志带 `wait` 与耗时 |
661: | `src/makeup/api.ts` | `pollLogin(ticket, waitSeconds=0)` |
662: | `src/makeup/LoginView.tsx` | 每 1.2 秒短轮询一次；成功立即 `onLogin`（不依赖定时器/会话计数）；页面显示"已检查 N 次 · 最近返回"，≥8 次给出 grep 命令 |
663: 
664: 为什么短轮询不会更慢：单次请求 <10ms，1.2 秒粒度对"扫码→进首页"的体感无差别；
665: 换来的是**没有长空闲连接**，以及**响应丢了也能补偿取回**。
666: 
667: ### 11.4 验收
668: 
669: - `PollIdempotencyTest`：`wait=0` 未确认必须 3 秒内回 409；确认后同 ticket 两次轮询返回同一 token，且该 token 可直接调 `/courses`。
670: - 全量 **40 个测试通过**。
671: - 本地实测日志（同 ticket 第二次轮询走缓存）：
672: 
673: ```
674: 扫码轮询成功 ticket=TK-f3c89 userId=... wait=0s 耗时=4ms
675: 扫码轮询命中缓存 ticket=TK-f3c89 userId=20****17
676: 扫码轮询成功 ticket=TK-f3c89 userId=... wait=0s 耗时=0ms
677: ```
678: 
679: ### 11.5 排障口径（以后看日志先看耗时）
680: 
681: | 日志 | 含义 |
682: |---|---|
683: | `扫码轮询暂未拿到凭据 ... 耗时=0ms` | 正常等待，短轮询每 1.2 秒一条，可忽略 |
684: | `扫码轮询成功 ... 耗时=xxms` | 后端已签发 token；前端还不动，就是前端问题（点「进入首页」） |
685: | `扫码轮询命中缓存` | 前端重试取回了同一个 token——这条出现就说明"响应丢失"被兜住了 |
686: | `收到登录成功凭据` / `换取 sessionid 失败，保留凭据` | 扫码消息到了但换取失败，凭据保留，下一次轮询重试 |
687: 
688: ---
689: 
690: ## 12. APK「进入课堂」改为真正进入官方会话（2026-11-25 09:45）
691: 
692: > 本节取代 §8「后端自渲染为默认方案」：自渲染只读 `lesson-summary`，不会在官方侧建立课堂会话
693: > （老师端不出现、无 WebSocket）。用户选定 APK 默认走 §7.2 ③。
694: 
695: ### 12.1 现在的路径
696: 
697: | 端 | 「进入课堂」默认行为 |
698: |---|---|
699: | APK | `join(enhance=false)` 拿固定入口 → `GET /lessons/entry-cookie` → 原生注入官方域 Cookie → App 内 WebView 打开 `/lesson/fullscreen/v3/{lessonId}?source=5` |
700: | APK（上一步抛错） | toast 提示后降级为后端自渲染，自渲染页内保留「重试官方课堂页」按钮 |
701: | Web | 无法注入官方域 Cookie，直接自渲染；页内 `window.open` 按钮需浏览器已登录雨课堂 |
702: 
703: 全程不调用 `checkin`，不经过动态码。后端无改动。
704: 
705: ### 12.2 改动位置
706: 
707: | 位置 | 改动 |
708: |---|---|
709: | `src/makeup/MakeupApp.tsx` | `openOfficialClassroom()` 原生优先走注入 + WebView，失败才 `fetchClassroomRender`；按钮提示语按端区分 |
710: | `ClassroomEntryPlugin.java` | 校验入口 URL 必须属于注入 Cookie 的官方域，否则 reject |
711: | `ClassroomWebViewActivity.java` | 主文档加载失败时显示错误说明，不再停在空白页 |
712: 
713: **仍未验证**：§7.5 的四项只能在真实开课时确认。`open()` 在 Activity 启动后即返回成功，
714: 不代表会话已建立；若 WebView 里被跳回官方登录页，说明 sessionid 失效或官方页不接受该 UA。
715: 
716: **排障**：`audit.log` 的 `ENTER_LESSON ... credential=fallback` 在本路径下属正常（默认不换凭证），
717: 不代表进课堂失败。
718: 
719: ---
720: 
721: ## 13. 抓取当前课程全部 PPT（含未播放页）（2026-11-25 11:20）
722: 
723: ### 13.1 为什么现有链路拿不全
724: 
725: `/content/slides` 只处理用户点开的那一个课时，且历史课件走 `lesson-summary/student/presentation`，
726: 老师**还没翻到**的页不会同步下来。用户要的是「整门课的全部 PPT，包括未播放的」，
727: 因此要同时解决两件事：
728: 
729: | 场景 | 必须走的路 |
730: |---|---|
731: | 进行中的课堂 | 官方全屏课堂页自己发的 `GET /v/lesson/lesson_info_v2?lesson_id=&source=5`，从 `data.presentationList[].presentation[]` 读**整份**课件页 |
732: | 历史 / 尚未上课的课时 | 遍历 `/v2/api/web/logs/learn/{cid}?actype=14` 的**全部**活动（含 `finished=false`），逐个用 `courseware_id` 拉 `lesson-summary` |
733: 
734: `download5` 系列是直播回放录像下载，不是 PPT；`/v/cards/ppt_download_url` 是原文件上传件，
735: 不能按页直接展示。所以没有走这两条。
736: 
737: ### 13.2 新接口
738: 
739: | 接口 | 说明 |
740: |---|---|
741: | `GET /api/v1/content/course-all?courseId=&useJwt=false` | 返回整门课的 `CoursewareBundle`：课时 → 课件 → 页；同时给出「官方声明页数 vs 实际拿到页数」与 warnings |
742: | `GET /api/v1/content/course-all.zip?courseId=&useJwt=false` | 直接流式打包所有页图片 + `manifest.csv` + `README.txt`，不落盘 |
743: 
744: `useJwt=true` 时，对缺页课件会用 `pptJwt` 调 `/api/v3/lesson/presentation/fetch` 补全
745: （通过 `YktInClassQuizApi.fetchFullSlides`）。**这会触发 checkin**，动态码课堂可能被拦截；
746: 默认关闭，由用户在页面上主动点「用课堂凭证补全缺页」。前端 ZIP 下载带 `X-Session-Token`，
747: 不能用普通 `<a href>`。
748: 
749: ### 13.3 数据结构
750: 
751: ```
752: CoursewareBundle
753: ├─ lessonCount / presentationCount / slideCount / missingPageCount / complete
754: ├─ lessons[]
755: │   ├─ title / finished / attended / declaredSlides / fetchedSlides / warning
756: │   └─ presentations[]
757: │       ├─ declaredCount / fetchedCount / source
758: │       └─ slides[] { page, imageUrl, problem, source=summary|jwt }
759: └─ warnings[]
760: ```
761: 
762: 前端 `CourseCenter.tsx`：课时列表新增「抓取全部 PPT（含未播放）」按钮 →
763: 全量清单视图（标记 进行中 / 已播放 / 未播放、缺页数、warning）→ 点任一课时复用现有 PPT 翻页视图，
764: 并可一键下载 ZIP。
765: 
766: ### 13.4 修复的附带缺陷
767: 
768: 09:45 那版 `SelfRenderedClassroom` 诊断面板引用了不存在的 `courses`，类型检查报错且面板一开会崩。
769: 已给组件补 `courses: Course[]` 入参并由父组件传 `allCourses`。
770: 
771: ### 13.5 验证与产物
772: 
773: - 新增 `FullCoursewareTest` 3 项：包含未播放课时、未登录 401、ZIP 有 PK 头与 manifest；全量 **43 测试通过**。
774: - `YktLiveLessonApi` 对 `presentationList[].presentation[]` 的字段名做了兼容，
775:   真实开课时若仍提示「未内嵌页列表」，按 warnings 里给出的结构补字段即可。
776: - 产物：`releases/ykt-backend-scheme4.jar`（11:20 重打）、`releases/gongxiaohu-v1.1.apk`（11:2x 重建，JS `main-D6SCPBTP.js`，水印「全量课件版（b3）」）。
777: 
778: ### 13.6 已知限制
779: 
780: - 官方课堂接口返回结构随版本变化；无法从本地资料确认所有字段名，warnings 会如实暴露。
781: - 未播放页若官方根本没下发图片直链，最多只能拿到页码占位（ZIP 里写 `.txt` 说明）。
782: - 抓取的是自己账号可见的内容，ZIP 含学号无关信息但仍是课件原文，请勿传播。
783: 
784: ---
785: 
786: ## 14. 当前课堂页按历史课堂对齐（2026-11-25 11:30）
787: 
788: ### 14.1 差距
789: 
790: §12 之后，APK 的「进入课堂」默认进官方会话；Web / 降级路径进入的是 `SelfRenderedClassroom`。
791: 它和历史课时页（`CourseCenter` 视图 3）有两个实质差距：
792: 
793: | 项 | 历史课堂页 | 原当前课堂页 |
794: |---|---|---|
795: | 课件范围 | 全量课件（含未播放页，见 §13） | 仅 `render` 接口返回的少量实时页 |
796: | 交互 | 题目置顶可跳页、全屏 / 横竖屏 / 双指缩放 / 滚轮翻页 / 页码输入 / 底部缩略图 | 只有上一页 / 下一页 + 一行小缩略图 |
797: 
798: ### 14.2 改动
799: 
800: 1. **数据**：`openOfficialClassroom()` 在拿到自渲染视图后，再调
801:    `fetchAllCourseware(courseId)`，优先取 **进行中课堂（LIVE-）** 的全量课件覆盖
802:    `view.presentations`；没有 LIVE 课时则用第一个课时。返回页数为 0 时保留原实时课件，
803:    失败只写 `view.hint` 提示，不阻断进入课堂。`useJwt` 仍为 false，不会隐式签到。
804: 2. **交互**：把历史课堂页的课件浏览能力完整移植进 `SelfRenderedClassroom`：
805:    - 课件内嵌题目置顶为「教师题目 · 测验区」，点击跳到对应课件页并高亮；
806:    - 全屏 / 横竖屏、双击切换、双指缩放平移、仅在 PPT 区域拦截滚轮翻页（260ms 节流）；
807:    - 页码输入框（按真实 `page` 匹配，退化按序号）、底部横向缩略图（含题橙点）；
808:    - 实时随堂题单独保留在下方，与课件内嵌题区分。
809: 3. **复用**：`CourseCenter` 中 `ProblemView`、`parseProblem()`、`ProblemCard` 改为导出，
810:    两个页面共用同一套题目解析与卡片，避免结构解释不一致。
811: 4. 验证诊断面板及其中的 `lesson_info_v2` 探测入口保留不变。
812: 
813: ### 14.3 验证与产物
814: 
815: - `tsc --noEmit` 通过（含 11:15 修复的 `courses` 引用缺陷）。
816: - 后端无改动，jar 仍为 §13 的 `releases/ykt-backend-scheme4.jar`。
817: - APK 重建：`releases/gongxiaohu-v1.1.apk`（JS `main-CZd-a_4t.js`），
818:   登录页水印「构建版本 2026-11-25 · 历史课堂同款版（b4）」。
819: 
820: ### 14.4 说明
821: 
822: 当前课堂页与历史课堂页的**视觉与交互同款**，但入口语义不同：
823: 历史课堂是「选一门课 → 选课时」，当前课堂是「进行中课程 → 自动载入 LIVE 课件」。
824: 若官方 `lesson_info_v2` 结构变化导致 LIVE 课件为空，会回落到第一个历史课时；
825: 页面上会显示实际载入的课时名与页数，便于核对。
826: 
827: ---
828: 
829: ## 15. lesson_info_v2 500 根因：缺身份头（2026-11-25 11:44）
830: 
831: ### 15.1 官方报错与根因
832: 
833: 探测返回 `{"errorcode":500,"errormsg":"'NoneType' object has no attribute 'set_cookie'}"`——
834: 官方服务端在 `lesson_info_v2` 处理中要写 cookie，但依赖的学生身份值是 None。
835: 
836: 两层原因：
837: 
838: 1. `GET /v/course_meta/user_info` 预检**没带 Cookie**（此时 sessionid 尚未换取），返回
839:    `{"op":"web_redirect","msg":"页面正在跳转"}`，`university_id`/`uv_id` 自然解析不到。
840: 2. 更根本：`university-id` / `classroom-id` / `uv-id` 本应从**课表数据**取（抓包实测
841:    `classroom-id:25529399`、`university-id:2803`、`uv-id:2803`，其中 university_id 来自
842:    `course.university_id`，uv_id 值等于它），而 `Course` 模型从未保存这两个字段，课表解析时丢掉了。
843: 
844: `lesson-summary` 历史课件只要 sessionid 所以能成功；`lesson_info_v2` 是课堂接口，需要完整身份头。
845: 
846: ### 15.2 修复
847: 
848: | 位置 | 改动 |
849: |---|---|
850: | `Models.Course` | 新增 `universityId` / `classroomId` 字段（保留旧 9 参构造器兼容） |
851: | `RealYktGateway.fetchCourseList` | 解析 `course.university_id` 与 `classroom_id` 存入 Course |
852: | `YktGateway.CourseInfo` | 新增 `universityId`；`findLiveLesson` 匹配 live 后从课表补齐 uni/cls，fallback 同样补齐 |
853: | `LessonController.lessonInfo` | 探测带 `X-UID`/`X-CSRFToken`/`university-id`/`uv-id`/`classroom-id`，ctx 缺省时从 CourseInfo 兜底 |
854: | `YktWebSocketLogin.exchangeSessionId` | user_info 双层解析 + web_login Set-Cookie 兜底 + 记录 Set-Cookie 清单（脱敏） |
855: 
856: ### 15.3 验证
857: 
858: 43 测试全通过；`releases/ykt-backend-scheme4.jar` 已重打（11:44）。
859: 
860: ### 15.4 下一步（需真实环境）
861: 
862: 部署新 jar → 重新扫码登录（让解析逻辑重跑）→ 再探测。若仍 500，日志里的
863: `uni=`/`cls=` 会显示是否已非空，加上「官方返回=」原文即可继续定位。

(End of file - total 863 lines)
</content>

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\正确实现链路.md</path>
<type>file</type>
<content>
1: # 2026-11-25 10:20 再次纠正：进课堂凭据来自 `lesson_info_v2`，不是 `checkin`，也不是「只靠 Cookie」
2: 
3: > 本节推翻本文下半部分（11-24 的推导）。下方旧记录保留作对照，**以本节为准**。
4: > 证据来源：技术博客原文（<https://blog.jiclub.site/2026/yuketang-deep-dive>）、
5: > 首页包 `backend/pc.js`、**官方课堂页自己的 JS**
6: > （`fe-static-yuketang.yuketang.cn/fe/static/lesson/3.3.241/js/fullscreen.b2d05c0f1c969680f5d5.js`，公开静态资源，未登录可下载）、
7: > 以及 10:01 真机截图。
8: 
9: ## 1. 分析过程
10: 
11: 1. **读博客原文**。§1 的原话是三步：请求课程表 → 过滤 `status == 1` → 「直接使用本地持久化的 `session_key` 换取课堂凭据一键入班」。
12:    博客说的是**换取课堂凭据**，从未说「不需要换取」。旧推导把这句理解成了「凭据就是 session_key 本身」。
13: 2. **查 `pc.js`**。`openFullscreen()` 只是 `window.open("/lesson/fullscreen/v3/"+lessonId+"?source=5")`——这只是**首页的跳转动作**，
14:    不代表课堂页加载后不再请求任何东西。旧推导到此就下了「无凭据交换」的结论，没有继续看课堂页。
15: 3. **下载并分析官方课堂页 JS**（此前从未做过）。这是决定性的一步，见下节。
16: 
17: ## 2. 官方学生端进课堂的真实流程（来自课堂页 JS）
18: 
19: ```
20: GET /v/lesson/lesson_info_v2?lesson_id={lessonId}&source=5      ← source 取自 URL 参数
21:   → data.userAuth            课堂凭据
22:   → data.userID / avatar
23:   → data.presentationList / quizList / activePresentationID / classroom
24:   → 页面调用 initws()
25: WSS wss://changjiang.yuketang.cn/wsapp/
26:   send {op:"hello", userid, avatar, role:"student", auth: userAuth, lessonid, presentation, ver:1}
27: ```
28: 
29: 课堂会话由 WebSocket `hello` 建立，`auth` 用的是 `userAuth`。这与博客「session_key 换取课堂凭据」完全对应。
30: 
31: 课堂页 JS 的其他事实：
32: 
33: | 事实 | 出处 |
34: |---|---|
35: | `/api/v3/lesson/checkin` 在课堂页里**只出现在接口表定义处，没有调用点** | 课堂页 JS 全文检索 |
36: | `50070:"该课堂已开启动态二维码签到，请扫码签到进班"` | 课堂页 JS 中文错误码表 |
37: | 全局响应处理器对 `50004` 和 `50070` **直接 return，不弹任何提示** | 课堂页 JS 响应拦截器 `h()` |
38: | `code === 50000` 时跳转 `/login/weixinapp/?next=<当前页>` | 同上 |
39: | 未登录访问 `fullscreen/v3/...` 仍返回 HTTP 200 的 SPA 壳（加载圈） | 实测 curl |
40: 
41: ## 3. 旧推导被推翻的四处
42: 
43: | 旧判断（11-24） | 事实 |
44: |---|---|
45: | 50070 对应 `lessonEndMsg`「老师已下课」，不是动态码专用码 | `lessonEndMsg` 是 `pc.js` 里 `notkn/checkin` 分支的处理；课堂页里 50070 明确就是动态二维码签到 |
46: | 动态码只约束签到考勤，不约束进入课堂 | 官方文案是「请扫码签到**进班**」；截图实测 `DYNAMIC_QR_CHECK_IN_REFUSED` |
47: | 课堂页不换凭据，凭 Cookie 直接建立会话 | 靠 `lesson_info_v2` 换 `userAuth`；该接口在本项目代码中出现 **0 次** |
48: | 课堂页收到 50070 仍可继续看课件 | 50070 被静默吞掉，页面大概率停在加载圈 |
49: 
50: 仍然成立的两点：`joinIfNotIn` 在官方 JS 中出现 0 次；`checkin` 是签到动作，不是学生进班接口。
51: 
52: ## 4. 10:01 截图逐项解读
53: 
54: | 现象 | 含义 |
55: |---|---|
56: | 自渲染页「课堂题目（0）」且无课件 | `lesson-summary` 对进行中的课堂没有数据，自渲染无法充当「进入课堂」 |
57: | `10:00:33 source=21 → code 50070` | 点「进入补答」触发，走 `/api/v3/lesson/checkin`（受动态码保护的签到接口） |
58: | 缺失头：X-CSRFToken / X-UID / classroom-id / university-id / uv-id | 该调用用的是仅含 sessionid 的最小上下文（`YktInClassQuizApi.checkinForJwt`） |
59: | 按钮文案「用官方课堂页打开」 | 运行的是 11-25 09:43 之前的 APK，默认走自渲染，固定入口 + Cookie 注入未被触发 |
60: 
61: **根因**：项目一直在调两个错误的接口——`checkin`（签到，被动态码拒绝）和 `lesson-summary`（归档总结，进行中为空），
62: 而官方学生端真正用的 `lesson_info_v2` 从未被调用。
63: 
64: ## 5. 尚未确定的一点
65: 
66: `lesson_info_v2` 在**开启动态码的课堂**上返回什么，只能真实开课时验证：
67: 
68: | 返回 | 结论 |
69: |---|---|
70: | `code=0` + `userAuth` | 在读学生可免扫码进班（博客路线），据此实现 |
71: | `code=50070` | 进班本身也被动态码拦截，只能本人扫码 |
72: 
73: 边界不变：若服务端返回 50070，不做绕过；签到考勤仍需本人当场扫码。
74: 
75: ## 6. 实测记录（10:30，`backend/auth.txt` 里的 sessionid）
76: 
77: | 请求 | 结果 |
78: |---|---|
79: | `GET /api/v3/classroom/on-lesson-upcoming-exam` | HTTP 200，`code=50000 UNAUTHENTICATED` |
80: | `GET /v/lesson/lesson_info_v2` | **未发出** —— 拿不到进行中的 lessonId，且该 sessionid 已失效 |
81: 
82: 结论：`auth.txt` 中的 sessionid 已过期（50000 即课堂页会跳转登录页的那个码），第 5 节的问题仍未验证。
83: 下一步需要一份有效 sessionid（扫码登录后从后端/手机会话取，或「导入 Cookie」），在真实开课期间重打这两条请求。
84: 
85: 为此新增了只读探测入口（10:35，jar 已重新打包，**服务器尚未部署**）：
86: `GET /api/v1/lessons/lesson-info?courseId=`（需 `X-Session-Token`）→ 用当前登录会话的完整 Cookie 头调 `lesson_info_v2`，
87: 只回 `code / msg / 是否有 userAuth / 课件数 / 题目数`。前端入口：诊断面板 →「进班探测」，选课后点「开始探测」。
88: 把结果（`code` 与 `hasUserAuth`）填回第 5 节的表格即可定论。
89: 
90: ---
91: 
92: # 以下为 11-24 旧推导（已被上方内容推翻，仅作对照）
93: 
94: 我之前的思路有个逻辑漏洞：把 `checkin` 同时当成"进课堂"和"签到"，而它恰好是被动态码卡住的那个接口——等于自己给自己设了门槛。我重新读博客原文和抓包记录，从链路本身重新推演。
95: 让我读博客原文和官方 JS 里的真实调用链——这是判断"动态码到底卡在哪一步"的关键。
96: 博客原文给了决定性线索。我重新读 §4.3 的接口实测表和官方 JS，找那条"在读学生专用通道"。
97: 博客原文很关键：**"这种扫码机制本质上是给旁听生使用的"** —— 动态码不是进课堂的门槛，是旁听生的进班通道。我之前把 `checkin` 当成必经之路，等于主动走进了旁听生通道。
98: 
99: 我重新查官方 JS 的实际调用参数，重点验证一个可疑点：我们一直传 `joinIfNotIn: true`。
100: 重大发现：`pc.js` 里 **50070 对应的是 `lessonEndMsg`（课堂结束提示），不是"动态码拒绝"**；而且那个 `checkin` 调的是 `notkn/checkin`（暗号加班级），不是 `/api/v3/lesson/checkin`。我之前把错误码语义搞错了。继续深挖官方进课堂的真实调用。
101: 决定性证据出来了。让我把几条关键线索串起来验证。
102: 这里有个颠覆性发现：`pc.js` 里 **50070 的文案是"老师已经下课了，不能加入课堂"**（`lessonEndMsg`），不是"动态码拒绝"；而且官方首页点"正在上课"是**直接 `window.open` 到 `/lesson/fullscreen/v3/{lessonId}?source=5`**，全程没有 checkin、没有 sign、没有动态码。我再确认几处。
103: 关键证据链已经变了。我再查签到接口和 50070 的全部语义，确认博客说的"自动签到"是否也绕开动态码。
104: 我再核对一下工作区里"动态码边界"这条结论的出处，避免误判。
105: 这里出现了决定性的认知错误。我再确认一处：`get-token` 到底属于哪张接口表。
106: 我把证据链彻底重查了一遍，发现**我之前的方案在根上就走错了**。先说结论，再给新方案。
107: 
108: ## 认知纠正：`checkin` 是签到通道，不是进课堂通道
109: 
110: 博客原文最关键的一句被我忽略了：
111: 
112: > 「这种扫码机制本质上是**给旁听生**使用的。对于已经在选课系统里的正规在读学生，学号本身就在班级学生名单库中……直接使用本地持久化的 `session_key` 换取课堂凭据一键入班。」
113: 
114: 而官方 `pc.js` 的实锤证据：
115: 
116: ```js
117: // 首页点「正在上课」的真实跳转
118: openFullscreen: function(p){
119:   ...
120:   window.open(location.origin + "/lesson/fullscreen/v3/" + A + "?source=5")
121: }
122: // 开课发现
123: getOnLesson: GET /api/v3/classroom/on-lesson-upcoming-exam → onLessonClassrooms[].lessonId
124: ```
125: 
126: **这条链路里没有 `checkin`、没有 `sign`、没有 `ts`、没有动态码。** 官方在读学生靠同源 Cookie（sessionid）直接打开固定入口，课堂会话由官方页面自己建立。
127: 
128: 我之前做的事：为了拿 `pptJwt` 去调 `POST /api/v3/lesson/checkin` —— 而 `checkin` 字面就是**签到**，正是动态码校验发生的那一步。等于**自己主动走进旁听生的签到通道，然后抱怨被动态码拦住**。方案 ④「后端自渲染」整体架在这个错误前提上。
129: 
130: 另外两处硬伤：
131: - `pc.js` 里 `50070` 对应 `lessonEndMsg`：「老师已经下课了，不能加入课堂」——**不是**动态码专用码；我把它单一归因了
132: - `joinIfNotIn` 在官方 JS 里出现 **0 次**，是开源自动签到脚本的参数，不是官方在读学生通道的参数
133: - 官方文档原话是「开启动态码时其他进班渠道均无法完成**签到**」——限制的是签到动作，不是进入课堂会话
134: 
135: 所以：**你的目标是对的，博客思路也是对的，是我把两条链路混成了一条。**
136: 
137: ## 新方案：三条候选，都不经过动态码
138: 
139: 核心原则：**把「进入课堂会话」与「签到」彻底解耦**，永不把 `checkin` 作为进课堂的前置。
140: 我再确认一个关键点：官方课堂页加载后靠什么建立会话，以及工作区是否有课堂页的 JS 可查。
141: 关键证据已闭环。我再做一次外部交叉验证，确认"在读学生固定入口无需签到"这一点，同时查 `lesson-info` 初始化时是否自带会话凭证。
142: 官方帮助文档给出了决定性佐证：**进入课堂的方式有 5 种，"正在上课"提示是已加入班级学生的独立通道，与扫码/暗号并列**。动态码只是签到/进入方式之一，不是门槛。
143: 
144: 我把重新推演的完整结论写成方案文档，并修正当前代码里那个错误的 50070 语义。
145: 官方帮助文档与 `pc.js` 双向印证了你的判断。我落地新方案：把 `checkin` 从"进课堂必经"降级为可选，主链路改走固定入口 + Cookie 注入。先看 Android 侧结构。
146: 证据链已经闭环，我确认之前的判断错了。现在落地新方案：把 `checkin` 从进课堂主链路移除，改走官方在读学生固定入口 + 原生 Cookie 注入。先建任务清单。
147: 先做后端解耦。核心改动：`join` 不再强制换凭证，凭证只在自渲染增强时按需尝试。
148: 我先重写进班服务的类注释和主逻辑，把"进课堂会话"与"签到换凭证"彻底解耦。
149: 现在改 `join()` 主逻辑：默认不调 checkin。我加一个显式的 `needCredential` 开关，默认 false。
150: 现在改凭证尝试块，让它受 `needCredential` 控制，并修正 hint 语义。
151: 现在改 LessonController：join 默认不签到，新增 `?enhance=true` 才尝试凭证；并加一个接口返回 Cookie 注入所需的官方域信息。
152: 现在修正 50070 的错误语义。
153: 同样修正类注释和 enterLesson 的 javadoc。
154: 编译并跑测试，确认解耦没破坏现有功能。
155: 33 个测试通过。现在做 Android 原生 Cookie 注入插件（Task #56）——这是"不经动态码进课堂"真正落地的关键。
156: Android 侧要用 App 内 WebView（不能用系统浏览器，CookieManager 注入的 Cookie 只对 WebView 生效）。我先确认工程结构。
157: 我写原生插件。设计：
158: - `ClassroomEntryPlugin`：JS 调用 `open({url, cookie, domain})`
159: - 用 `CookieManager.setCookie` 给官方域注入 sessionid/csrftoken
160: - 启动 App 内 WebView Activity 加载固定入口（系统浏览器不共享 App 的 Cookie）
161: 
162: Web 端无此插件时降级到同源反向代理方案。
163: 注册插件到 MainActivity，并在 Manifest 声明 Activity。
164: 现在需要一个后端接口，让 APK 能拿到注入用的完整 L1 Cookie（不下发到任意网页，需鉴权）。
165: 我用了 `applicationContext` 但没注入。改成直接注入 AppProperties，更规范。
166: 编译测试。
167: 后端通过。现在前端（Task #57）：加插件调用封装和入口 Cookie 接口，主页按钮优先用原生注入。
168: 现在写原生插件的 JS 封装。Capacitor 插件用 `registerPlugin`，Web 端不可用时有降级。
169: 现在改 MakeupApp 的进班逻辑：原生端优先走固定入口 + Cookie 注入，Web 端/失败时降级到自渲染。
170: 还要在自渲染页把"在浏览器打开官方课堂页"按钮改得更准确（Web 端因跨域 Cookie 会 401，需提示）。
171: 构建验证前端。
172: 前端通过。现在编译 APK，验证原生插件能编进去。
173: 编译中。我同时写方案文档（Task #58）。
174: 修正 §3.3 和 §6 里那些错误表述，加指向 §7 的提示，避免文档自相矛盾。
175: APK 编译成功（原生插件已编入）。我修正 §6 开头的总括说明，然后打包后端 jar。
176: 打包后端 jar 并复制 APK。
177: 我补一个针对新链路的测试（断言默认 join 不触发 checkin），同时等打包。
178: 35 个测试全通过。我做最终验证：确认 jar 与 APK 都是最新源码，且原生插件确实编进了 APK。
179: `固定入口` 那个 ✗ 跟之前一样是误报——URL 由后端生成。我确认后端字符串和 APK 里的 dex 插件类。
180: 全部验证通过。更新工作日志并收尾。
181: 你是对的，我之前的方案在根上就错了。已重新推演并落地，交付三个产物（见上方卡片）。
182: 
183: ## 错在哪
184: 
185: 我把 `POST /api/v3/lesson/checkin` 当成"进入课堂会话"的必经凭证来源。但 `checkin` 字面就是**签到动作**——为了拿 `pptJwt` 主动调它，等于自己走进旁听生的签到通道，被 50070 拦住后还误判成"动态码不让进课堂"。
186: 
187: **硬证据**（重查后）：
188: 
189: | 来源 | 结论 |
190: |---|---|
191: | 博客 §1 原文 | 「这种扫码机制本质上是给**旁听生**使用的」；在读学生学号在名单库，用 `session_key` 直接一键入班 |
192: | 官方 `pc.js` | `getOnLesson()` 拿 `onLessonClassrooms[].lessonId` → `openFullscreen()` 直接 `window.open("/lesson/fullscreen/v3/"+lessonId+"?source=5")`，**无 checkin、无 sign、无 ts、无动态码** |
193: | 官方帮助文档 | 列出 5 种进课堂方式，「正在上课」提示是已加入班级学生的**独立通道**；动态码限制的是**签到考勤** |
194: | `pc.js` 错误处理 | 50070 对应 `lessonEndMsg`「老师已经下课了，不能加入课堂」，不是动态码专用码 |
195: | `pc.js` 全文 | `joinIfNotIn` 出现 **0 次**（开源自动签到脚本的参数）；`checkin` 接口有定义但**无调用点** |
196: 
197: ## 新方案：不经动态码进入课堂会话
198: 
199: ```
200: L1 sessionid + csrftoken
201:   → GET /api/v3/classroom/on-lesson-upcoming-exam 拿 lessonId
202:   → 官方固定入口 /lesson/fullscreen/v3/{lessonId}?source=5（同源 Cookie）
203:   → 课堂会话建立，全程无签到、无动态码
204: ```
205: 
206: **真正的门槛是跨域 Cookie，不是动态码**。三种形态按端选：APK 用原生 WebView 注入（已实现）、Web 用同源反向代理、调试用 CDP。关键坑：`CookieManager` 注入的 Cookie **只对 App 内 WebView 生效**，系统浏览器不共享，所以必须用独立的 WebView Activity 承载官方页。
207: 
208: ## 落地改动
209: 
210: - **后端**：`join()` 默认**不再调 checkin**，新增 `needCredential` 开关（仅自渲染增强时按需尝试，失败不阻断进课堂）；`/lessons/join?enhance=false`、`GET /lessons/entry-cookie`；修正 50070 文案与 `outcome` 为 `CHECKIN_REFUSED`
211: - **Android 原生**：新增 `ClassroomEntryPlugin`（Cookie 注入）+ `ClassroomWebViewActivity`（JS/DOM/桌面 UA/第三方 Cookie），注册到 MainActivity
212: - **前端**：`classroomEntry.ts` 封装原生调用；主页「进入课堂」优先走固定入口 + 注入，失败才降级自渲染
213: - **文档**：技术说明新增 §7（认知纠正、正确链路、Cookie 三形态、落地清单、待验证项），并修正 §3.3 / §6 的错误表述
214: 
215: **35 个测试全通过**；已验证 jar 内含固定入口、APK dex 内含三个插件类。
216: 
217: ## 边界与待验证
218: 
219: 动态码**签到考勤**仍需本人当场扫码，本项目不做也不绕过——这个边界没变。
220: 
221: 下次真实开课请重点验证 4 项（§7.5）：桌面 UA 是否被官方页接受、WebSocket 会话能否建立、会话建立后 `lesson-summary` 是否解锁实时课件、H5 入口 `/m/v2/lesson/student/` 是否更适合移动端。诊断面板已能记录证据，建议清空后分别点「原生进入」和「自渲染增强」各一次做对比。
222: 
223: **服务器需换新 jar** 才有 `entry-cookie` 和 `enhance` 参数。

(End of file - total 223 lines)
</content>

[assistant]
Now read 尝试策略.md and 日志获取.md. Then "读取文件信息" — the project structure. Maybe list root files and key dirs. pwsh is broken (ACL). Let me consider fixing with the diagnose skill. But maybe the user means read the file info of the .md docs? "先读取.md文档,再读取文件信息" — could mean read the md docs, then read project files. Ambiguous. Probably a standard prompt in this workspace instructing the agent to read docs then inspect the project.

Let me read the remaining two docs.

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\尝试策略.md"}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\日志获取.md"}

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\尝试策略.md</path>
<type>file</type>
<content>
1: 截图信息量很大，我先对照技术博客原文和 `pc.js` 的接口表，核实「固定入口不需要签到」这个前提到底有多少证据。
2: 
3: 博客原文拿到了，有两点和《正确链路》的解读不一致，我再查 `pc.js` 里剩下的课堂接口和它们的调用点。
4: 
5: 关键线索：官方课堂页的 JS 是公开静态资源，之前文档里只分析了首页 `pc.js`，从没看过课堂页自己加载后调了什么。把它下载下来分析。
6: 
7: 重要发现：官方课堂页自己的 JS 里就有 `/api/v3/lesson/checkin`，说明「课堂页不调 checkin」这个前提可能是错的。看它的调用参数和对 50070 的处理。
8: 
9: 决定性证据出现了：官方课堂页里 `50070` 的原文就是「该课堂已开启动态二维码签到，请扫码」。再看课堂页加载时到底怎么调 `checkin`、收到 50070 后走什么界面。
10: 
11: 学生端课堂页里 `checkin` 只出现在接口表定义处，没有调用点。接着查课堂页如何拿课堂态凭据（`set-auth`）、遇到动态码时显示什么。
12: 
13: 官方原文是「该课堂已开启动态二维码签到，请扫码签到进班」。再看课堂页的初始化顺序和 WebSocket 建立方式，确认凭据从哪来。
14: 
15: 找到学生端真正的进班接口了：不是 `checkin`，而是 `getPresentationList`。看它的路径、参数和失败处理。
16: 
17: 官方学生端的真实进班流程已经还原出来。最后确认一下仓库里现成的调试凭据能否直接验证这个接口（只看结构，不输出内容）。
18: 
19: 分析完了，这轮没有改代码。结论：《正确链路》的核心前提不成立，官方课堂页进入课堂时要换一次课堂凭据，而项目从来没调过那个接口。
20: 
21: ## 官方课堂页的真实流程
22: 
23: 这次直接分析了官方课堂页自己的 JS（`fullscreen` 包，v3.3.241，公开静态资源）。之前文档只看过首页的 `pc.js`。
24: 
25: 1. 页面加载后请求 `GET /v/lesson/lesson_info_v2?lesson_id={lessonId}&source=5`。
26: 2. 响应里的 `userAuth` 就是课堂凭据，还带回课件列表、题目列表和当前课件 ID。
27: 3. 再用 `userAuth` 连 `wss://…/wsapp/` 发 `hello`，课堂会话才建立。
28: 
29: 这正好对上博客原文「用 `session_key` 换取课堂凭据一键入班」。博客也从没说过「不需要换取凭据」。
30: 
31: ## 《正确链路》被推翻的四处
32: 
33: | 原判断 | 官方 JS 实际 |
34: |---|---|
35: | 50070 是「老师已下课」，不是动态码专用码 | 课堂页原文：`50070:"该课堂已开启动态二维码签到，请扫码签到进班"` |
36: | 动态码只约束签到，不约束进课堂 | 官方文案是「扫码签到**进班**」，进班本身受控 |
37: | 课堂页不换凭据，靠 Cookie 直接进 | 靠 `lesson_info_v2` 换 `userAuth`，项目代码里这个接口出现 0 次 |
38: | 收到 50070 页面会正常继续 | 全局处理器对 50070 静默吞掉不提示，页面大概率停在加载圈 |
39: 
40: `pc.js:1044` 的 `openFullscreen` 确实只是 `window.open`，但那只是首页的跳转动作。我上一轮只核对了 URL 写法就说「一致」，没查课堂页加载后的行为，这是我的疏漏。
41: 
42: ## 截图逐项解读
43: 
44: - **自渲染页 0 课件 0 题目**：`lesson-summary` 对进行中的课堂没有数据，自渲染当不了「进入课堂」。
45: - **10:00:33 的 checkin**：是点「进入补答」触发的，走 [YktInClassQuizApi.java:71](file:///D:/workspace/tools/YuketangAssistant/backend/src/main/java/com/jiclub/gongxiaohu/gateway/YktInClassQuizApi.java#L71) 的最小上下文，缺 5 个头，而且 `checkin` 本来就是受动态码保护的签到接口。
46: - **版本**：截图里的按钮还是旧文案「用官方课堂页打开」，说明跑的是我改动之前的 APK，默认走自渲染。
47: 
48: ## 仍未确定的一点
49: 
50: `lesson_info_v2` 在开启动态码的课堂上返回什么，只有真实开课时打一次才知道：
51: 
52: - 返回 `code=0` 和 `userAuth`：在读学生确实能免扫码进班，这是博客的路线。
53: - 返回 50070：进班这一步也被动态码挡住，只能本人扫码，没有技术手段可绕。
54: 
55: ## 下一步
56: 
57: 我打算两件事一起做：
58: 
59: 1. 后端加一个只读探测，调 `lesson_info_v2` 并记录业务码、是否有 `userAuth` 和课件数（诊断面板可见，不记明文）。
60: 2. 如果成功，让「进入课堂」改用它建立课堂会话。
61: 
62: [backend/auth.txt](file:///D:/workspace/tools/YuketangAssistant/backend/auth.txt) 里有一个 sessionid，如果现在正在上课，我可以马上用它打一次 `lesson_info_v2`，几分钟就有结论。要我现在打吗？
63: 
64: Sources
65: - [工小湖：给雨课堂和学校教务做了个小程序](https://blog.jiclub.site/2026/yuketang-deep-dive)
66: - [雨课堂官方课堂页 JS（fullscreen 3.3.241）](https://fe-static-yuketang.yuketang.cn/fe/static/lesson/3.3.241/js/fullscreen.b2d05c0f1c969680f5d5.js)

(End of file - total 66 lines)
</content>

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\日志获取.md</path>
<type>file</type>
<content>
1: 
2: 
3: 命令
4: ```aiignore
5: # 服务器上：最近 20 次登录，直接看到学号
6: grep LOGIN_OK /opt/ykt-backend/logs/audit.log | tail -20
7: 
8: # 某学号的全部行为（登录+进课堂+登出）
9: grep 'userId=2023012345' /opt/ykt-backend/logs/audit.log
10: 
11: # 实时运行日志 / 只看错误
12: tail -f /opt/ykt-backend/logs/app.log
13: grep -E ' (ERROR|WARN) ' /opt/ykt-backend/logs/app.log | tail -50
14: 
15: # 打包 + 拉回本地
16: ssh root@******** "tar -czf /tmp/ykt-logs.tar.gz -C /opt/ykt-backend/logs ."
17: # 只有审计日志（audit.log + 历史 audit.*.gz）
18: scp root@********:/tmp/ykt-logs.tar.gz .
19: # 当前 audit.log  登录登出    app.log  运行日志
20: scp root@********:/opt/ykt-backend/logs/audit.log .
21: 
22: 
23: ```

(End of file - total 23 lines)
</content>

[assistant]
Now "再读取文件信息" — read file information. I should inspect the project structure/files. pwsh is broken due to ACL failure. Let me try the diagnose skill? That's for sandbox ACL denials and has a bundled command. But running a command requires pwsh which itself is blocked at grantWrite step... The error says "sandbox-local: windows-acl workspace grant failed". That happens before command runs.

Maybe I can retry with danger-full-access permission which would ask the user. Alternatively, I can inspect via read/glob tools which work fine (they use the filesystem backend, not sandbox? apparently they work).

Let me look at the project structure using glob on specific directories, and read key files: package.json, metadata.json, backend/pom.xml, application.yml, .env.example, capacitor.config.ts.

Actually maybe "文件信息" means "file information" of the .md documents? Hmm. In Chinese, "读取文件信息" likely means inspect the workspace files. Let me get a directory-level view. I'll glob top-level dirs by matching pattern "*/*" — too many files. Better: read the glob spill file filtered.

Alternative: try pwsh again — maybe transient. Let me try with a simple command.

Let me also just read metadata.json and package.json.

[assistant • pwsh]
{"command":"Get-ChildItem -Force -Name","description":"List top-level names"}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\package.json"}

[tool • pwsh]
Error: sandbox-local: windows-acl workspace grant failed: Win32Error: SetNamedSecurityInfoW failed (Win32 5): grantWrite(D:\workspace\tools\YuketangAssistant). The command did not run; check that the current user can change permissions on the workspace and temp directories, or run with danger-full-access.

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\package.json</path>
<type>file</type>
<content>
1: {
2:   "name": "gongxiaohu",
3:   "private": true,
4:   "version": "3.0.0",
5:   "type": "module",
6:   "scripts": {
7:     "dev": "vite --port=3000 --host=0.0.0.0",
8:     "build": "vite build",
9:     "preview": "vite preview",
10:     "clean": "rm -rf dist server.js",
11:     "lint": "tsc --noEmit"
12:   },
13:   "dependencies": {
14:     "@capacitor/android": "^8.6.2",
15:     "@capacitor/cli": "^8.6.2",
16:     "@capacitor/core": "^8.6.2",
17:     "@google/genai": "^2.4.0",
18:     "@tailwindcss/vite": "^4.3.3",
19:     "@vitejs/plugin-react": "^6.1.1",
20:     "dotenv": "^17.2.3",
21:     "express": "^4.21.2",
22:     "lucide-react": "^0.546.0",
23:     "motion": "^12.23.24",
24:     "react": "^19.0.1",
25:     "react-dom": "^19.0.1",
26:     "vite": "^8.3.0"
27:   },
28:   "devDependencies": {
29:     "@types/express": "^4.17.21",
30:     "@types/node": "^22.14.0",
31:     "@types/react": "^19.3.0",
32:     "@types/react-dom": "^19.3.0",
33:     "autoprefixer": "^10.4.21",
34:     "esbuild": "^0.25.0",
35:     "tailwindcss": "^4.3.3",
36:     "tsx": "^4.21.0",
37:     "typescript": "^7.0.2"
38:   }
39: }

(End of file - total 39 lines)
</content>

[assistant]
pwsh still fails. Should I fix it? The user asked to "读取文件信息". I can do that via read/glob. But a broken shell is a significant environment problem worth reporting and possibly fixing with the diagnose skill. Let me first gather structure info.

Let me use glob with patterns for directories: e.g. "backend/src/**/*.java" and "src/**/*.tsx", "miniprogram/**", "deploy/*", "releases/*". And read metadata.json.

Actually, since the glob tool returns files sorted, I can also grep the spilled 5651 file for top-level patterns. Let me grep the full listing for lines like `backend\src\main\java\...` — but that's huge.

Better approach: read specific key config files:
- metadata.json
- capacitor.config.ts
- .env.example
- backend/pom.xml
- backend/src/main/resources/application.yml
- backend/README.md
- deploy/README.md

And get file listing of backend java + src + miniprogram via glob.

[assistant • glob]
{"pattern":"backend/**/*.java"}

[assistant • glob]
{"pattern":"src/**/*.{ts,tsx,css}"}

[tool • glob]
backend\src\main\java\com\jiclub\gongxiaohu\GongXiaoHuApplication.java
backend\src\main\java\com\jiclub\gongxiaohu\config\WebConfig.java
backend\src\main\java\com\jiclub\gongxiaohu\common\ApiResponse.java
backend\src\main\java\com\jiclub\gongxiaohu\common\BizException.java
backend\src\main\java\com\jiclub\gongxiaohu\common\GlobalExceptionHandler.java
backend\src\main\java\com\jiclub\gongxiaohu\util\SignUtil.java
backend\src\main\java\com\jiclub\gongxiaohu\service\SessionResolver.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\AnswerController.java
backend\src\main\java\com\jiclub\gongxiaohu\service\MakeupAnswerService.java
backend\src\test\java\com\jiclub\gongxiaohu\RealLoginProtocolTest.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktExerciseApi.java
backend\src\test\java\com\jiclub\gongxiaohu\AuthFlowTest.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\PreviewController.java
backend\src\test\java\com\jiclub\gongxiaohu\PreviewParseTest.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktPreviewApi.java
backend\src\test\java\com\jiclub\gongxiaohu\InClassQuizTest.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\InClassQuizController.java
backend\src\main\java\com\jiclub\gongxiaohu\config\AppProperties.java
backend\src\test\java\com\jiclub\gongxiaohu\NewFeaturesTest.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktPasswordLoginApi.java
backend\src\test\java\com\jiclub\gongxiaohu\PasswordLoginTest.java
backend\src\test\java\com\jiclub\gongxiaohu\ContentCenterTest.java
backend\src\main\java\com\jiclub\gongxiaohu\model\ContentModels.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktContentApi.java
backend\src\main\java\com\jiclub\gongxiaohu\model\AuthModels.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\LessonDiagnostic.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\DiagnosticsController.java
backend\src\test\java\com\jiclub\gongxiaohu\DiagnosticsTest.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\LessonCredentialClient.java
backend\src\test\java\com\jiclub\gongxiaohu\SmokeTest.java
backend\src\main\java\com\jiclub\gongxiaohu\service\InClassQuizService.java
backend\src\main\java\com\jiclub\gongxiaohu\common\RequestContext.java
backend\src\main\java\com\jiclub\gongxiaohu\common\AccessLogFilter.java
backend\src\main\java\com\jiclub\gongxiaohu\common\AuditLog.java
backend\src\test\java\com\jiclub\gongxiaohu\LoggingAuditTest.java
backend\src\test\java\com\jiclub\gongxiaohu\gateway\YktWebSocketLoginFlowTest.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\MockYktGateway.java
backend\src\main\java\com\jiclub\gongxiaohu\service\AuthService.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\AuthController.java
backend\src\test\java\com\jiclub\gongxiaohu\PollIdempotencyTest.java
backend\src\main\java\com\jiclub\gongxiaohu\service\JoinLessonService.java
backend\src\main\java\com\jiclub\gongxiaohu\model\CoursewareModels.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktInClassQuizApi.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktLiveLessonApi.java
backend\src\main\java\com\jiclub\gongxiaohu\service\FullCoursewareService.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\ContentController.java
backend\src\test\java\com\jiclub\gongxiaohu\FullCoursewareTest.java
backend\src\main\java\com\jiclub\gongxiaohu\model\Models.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktGateway.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\YktWebSocketLogin.java
backend\src\main\java\com\jiclub\gongxiaohu\controller\LessonController.java
backend\src\main\java\com\jiclub\gongxiaohu\gateway\RealYktGateway.java

[tool • glob]
src\App.tsx
src\components\AndroidClient.tsx
src\components\MiniProgramClient.tsx
src\components\SlideCanvasRenderer.tsx
src\data\gongxiaohuData.ts
src\index.css
src\main.tsx
src\makeup\main.tsx
src\makeup\classroomEntry.ts
src\makeup\api.ts
src\makeup\CourseCenter.tsx
src\makeup\MakeupApp.tsx
src\makeup\LoginView.tsx

[assistant]
Now get top-level directories and key non-java files. Let me read metadata.json, capacitor.config.ts, application.yml, backend/pom.xml quickly. And glob miniprogram and deploy.

Note "读取文件信息" may just mean these. Let me also check git status? pwsh broken.

Let me try the ACL diagnostic skill — it's designed exactly for this: "Use on Windows for unexpected DSH sandbox access denials: workspace writes or listing fail". This is a real environment failure. Loading it gives instructions.

But priority: user asked read docs then file info. I'll gather file info with read/glob, then mention shell failure.

Let me batch reads.

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\metadata.json"}

[assistant • read]
{"file_path":"D:\\workspace\\tools\\YuketangAssistant\\backend\\src\\main\\resources\\application.yml"}

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\metadata.json</path>
<type>file</type>
<content>
1: {
2:   "name": "工小湖 · 雨课堂与智慧教务三端套件",
3:   "description": "基于雨课堂核心协议深度重构的轻量客户端套件，支持免扫码秒进班、30秒阻塞长轮询鉴权、超时补答与实时草稿防丢、虚拟列表+WebP课件流及随堂习题提前透视，提供官方网站、微信小程序端与Android客户端完整交互体验。",
4:   "requestFramePermissions": [],
5:   "majorCapabilities": ["MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API"]
6: }

(End of file - total 6 lines)
</content>

[tool • read]
<path>D:\workspace\tools\YuketangAssistant\backend\src\main\resources\application.yml</path>
<type>file</type>
<content>
1: server:
2:   port: 8080
3: 
4: spring:
5:   application:
6:     name: ykt-backend
7:   servlet:
8:     multipart:
9:       max-file-size: 5MB
10:       max-request-size: 45MB   # 9 张 * 5MB
11: 
12: # 工小湖后端配置
13: gongxiaohu:
14:   # 演示模式：mock = 使用内置假数据，开箱即用；real = 调用真实雨课堂接口
15:   # 当前学校为「长江雨课堂」，已切换为 real + changjiang.yuketang.cn
16:   mode: real
17: 
18:   # 本地防误触 / 幂等设置（§6.3 ③ 降级说明）
19:   # 注意：官方 join_lesson 的 sign 由服务端密钥签发，客户端无法构造也不应构造。
20:   # 这里的 secret 只用于本地 verify-sign 幂等判断与进班会话 TTL，官方不认此签名。
21:   join-signature:
22:     secret: dev-only-secret-change-me   # 生产环境请替换为随机密钥，勿提交到仓库
23:     ttl-seconds: 300                     # 自渲染会话(entryId)有效期 5 分钟
24: 
25:   # 真实模式下网关配置（mode: real 时生效）
26:   # 说明：除 auth-status 与 join-lesson 外，以下路径来自博客描述/约定，
27:   # 真实接口路径需根据你自己的抓包结果在配置文件或启动参数中覆盖。
28:   ykt-gateway:
29:     # 长江雨课堂（region=changjiang）：网页版 changjiang.yuketang.cn，扫码走其 wsapp。
30:     # 2026-11-23 实测：该域名 WebSocket 可返回真实微信二维码，user_info 返回 csrftoken，
31:     # courses/list 返回 401 "Cookie has no sessionid"（证明需 sessionid）。四个区域会话互不通用。
32:     region: changjiang                          # standard | hetang | changjiang | huanghe
33:     base-url: https://changjiang.yuketang.cn
34:     ws-url: wss://changjiang.yuketang.cn/wsapp/
35:     connect-timeout-ms: 10000
36:     read-timeout-ms: 35000               # 与 30 秒长轮询匹配
37:     endpoints:
38:       auth-qrcode: /custom/auth/v1/qrcode              # 抓包待确认：官方二维码 ticket 接口
39:       course-list: /v2/api/web/courses/list           # ✅实测可用：identity=2，返回 data.list
40:       live-list-v3: /api/v3/classroom/on-lesson-upcoming-exam  # ✅来源开源项目：正在上课+待考试
41:       lesson-detail: /v2/web/lesson/detail             # 抓包待确认
42:       join-lesson: /v2/web/join_lesson                 # 博客第 1 节
43:       quiz-detail: /v2/web/quiz/detail                 # 抓包待确认
44:       quiz-list: /v2/web/quiz/list                     # 抓包待确认
45:       submit-answer: /v2/web/quiz/submit               # 抓包待确认
46:       auth-status: /custom/auth/v1/auth/status         # 博客第 2 节（本版未启用）
47: 
48:   # 账号密码登录（RSA 公钥与滑块 appid 来自官方前端 login.js；滑块需用户手动完成）
49:   password-login:
50:     endpoint: /pc/login/verify_pwd_login/
51:     captcha-app-id: "2091064951"
52:     rsa-public-key: "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCQBaPX7crEH6/jS4hRD7lZrsFRIdfwEhH30onFnrnWxiRATzP9WEneXJEZHopmzudkNS5bDp51SCnBUGGgfL/sUUrlrhV2xnTSe1jRl924ejV5rkVkiii85jp9G8eJrJN6klHs0PfYfp4EVJ8688qpi5iETtg+q4ITocyEyD1+7wIDAQAB"
53: 
54:   # 草稿图上传本地存储目录（相对路径或绝对路径）
55:   upload-dir: ./data/draft-images
56: 
57: logging:
58:   # 日志分流见 logback-spring.xml：
59:   #   logs/app.log   —— 运行/诊断日志（学号脱敏，14 天）
60:   #   logs/audit.log —— 登录/进课堂审计（学号明文，180 天）
61:   # 目录默认 ./logs，生产用环境变量 LOG_DIR 指定（systemd 里 /opt/ykt-backend/logs）
62:   level:
63:     # 修正：实际包名是 com.jiclub.gongxiaohu（旧值 site.* 导致 debug 从未生效）
64:     com.jiclub.gongxiaohu: debug
65:     ACCESS: info        # 每请求一行访问日志；嫌吵可调 warn
66:     AUDIT: info
67:   pattern:
68:     console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"

(End of file - total 68 lines)
</content>

[assistant]
Now file info: let's get structure of major dirs: miniprogram, deploy, releases, android. Also check whether the current build artifacts exist and timestamps. Without shell, glob gives modification order (but not timestamps). Hmm, "文件信息" likely means file metadata (size/time). Without shell that's hard.

Maybe I should fix the shell. Let me load the diagnose skill.

Actually, wait — perhaps the ACL failure is because the workspace directory ACL grant needs admin. The skill exists precisely for this. Let me load it.

[assistant • skill]
{"name":"diagnose-windows-sandbox-acl"}

[tool • skill]
<skill_content name="diagnose-windows-sandbox-acl">
<skill_resources>
Base directory for this skill: C:\Users\Zhao\AppData\Local\Temp\dsh-acl-skill-5Sj9QI
Resolve relative paths mentioned by this skill against the base directory before using them. Load referenced resources only as needed.
</skill_resources>

<skill_instructions>
# Diagnose Windows sandbox ACL failures

**Write every approval request in plain words, in the user's language.** The prompt is all the user reads before widening access, so it must stand alone: which folder the script touches, that it adds the signed-in user's full-control entry where a right is missing and removes foreign package entries, that file contents and owners are unchanged, and that the printed recovery command undoes each change. Keep error codes, `WRITE_DAC`/`WRITE_OWNER`, `S-1-15-2-*` SIDs, `icacls`, ACL, ACE, verdict names and switches out of the request: write Windows file permissions, not ACL or ACE. Ask once, for one command.

## One command diagnoses and repairs

The script has no modes. `-Path`, `-AllowRoot` and `-Out` read the path and every ancestor and repair what the observations prove, in the same run:

- directories on that chain that lack effective `WRITE_DAC` or `WRITE_OWNER` receive a full-control allow ACE for the signed-in user, because DSH cannot provision its workspace grant without them;
- explicit AppContainer package allow ACEs (`S-1-15-2-*`, except the well-known groups ending in 1 or 2) are removed at their sources, ancestor first, which also removes those packages' access;
- a directory the sandbox cannot provision — the state its provisioning error reports on the workspace root — or the authorized root itself also has its subtree searched for those ACEs in the same run, so a deeper entry needs no second request. That bounded walk reports `truncated` and unreadable directories; if truncated, pass the still-failing deeper path once more.

Every change is backed up, then verified by re-reading it. `-AllowRoot` bounds all of it: an object is changed only when it is that directory or strictly inside it, so a workspace root can repair itself. Never split this into a diagnostic call and a repair call, never ask twice for one repair, and never pass a mode switch that does not exist.

```powershell
& '<skill-directory>\scripts\diagnose-windows-sandbox-acl.ps1' -Path '<failing-path>' -AllowRoot '<authorized-directory>' -Out '<recovery-directory>'
exit $LASTEXITCODE
```

Substitute full, quoted paths. Keep `-Out` persistent and user-owned, preferably beside the failing workspace; never use the skill resource directory, which is deleted when the skill unloads. Pass one path per invocation.

## Run it

Repeat the failing operation once confined. The script writes permissions, which the confined token cannot do — run it there and it would report the sandbox's own restriction as a missing right — so request approval for that one command and run it unconfined. A confined run of the script is never useful. Unconfined does not elevate the Windows token.

Diagnose unexpected denials of workspace writes, listing, or plainly readable paths; explain expected ones instead: writes outside the workspace, any write in `read-only`, piped grandchild `spawn EPERM`, ConstrainedLanguage errors. If approval is refused or unavailable, report the path as undiagnosed and stop.

## Read the output

Every record reaches stdout, the `acl-report-*.jsonl` file under `-Out`, and the final `RECAP` line, which carries the verdicts, changes, verifications, refusals and scans. Tool output keeps only its tail, so read the recap first, and read specific records from the report when it is not enough. Trust `verification` records, never `completed` actions. Decide from `details.nextAction`:

| `nextAction` | Meaning |
|---|---|
| `verify_original_confined_operation` | Repairs verified; repeat the original operation confined. |
| `stop` | Nothing repaired, or a refusal ended the run. Report and stop. |
| `restore_pending_then_stop` | Rollback unverified. Run the printed recovery commands in order, then stop. |

A deny ACE's presence alone does not establish causation, and the script never removes one; DSH's `S-1-4-*` grants and the Everyone `DeleteSubdirectoriesAndFiles` deny are expected, not conflicts. A deny that blocks the repair ends the run without a repair (`REPAIR_REFUSED`, or `GRANT_FAILED`) after restoring what it attempted. Each change leaves two files in `-Out` (`acl-backup-<id>.json` and its `.ps1`), and the run prints the matching `ROLLBACK` command.

## After a repair, and when it stops

Repeat the original failed operation confined. A verified repair is not undone because the original operation fails for a further reason: continue from the new observations, and if a deeper path is still denied, run the same one command there — again one call, one approval.

**Stop after any failed or refused repair, or failed verification.** The script already restored that invocation's changes; do not repeat it or start another repair, and never remove a deny ACE by hand.

**A stopped run still owes the user a decision.** Name the blocking object and the right or ACE it lacks, what changed and what was rolled back, the recovery commands in order, and the report path. Only the user can lift confinement, so ask them to switch this session to **full access** (Chinese UI: 完全权限) temporarily, and say what that opens: keep working under it, or ask you to keep investigating this file-permission problem, since the report already records the mechanism and the evidence. Then ask them to send this session as feedback, quoting the report records that matter, so the unhandled scenario reaches us — only what reaches this conversation travels with it. For example:

> 这个工作区被 Windows 文件权限挡住了：`<对象>` 上 `<缺哪个权限 / 哪条权限项>`。你可以把本会话切到「完全权限」(full access) 临时继续工作；切完之后既能直接干活，也可以让我继续排查这个文件权限问题（报告里已经有机制和证据）。也麻烦把这个会话作为反馈发出去，好让我们补上这个场景。
>
> This workspace is blocked by Windows file permissions: `<object>` `<missing right / offending entry>`. Switch this session to full access (完全权限) to keep working meanwhile; then either continue your work or have me keep investigating this file-permission problem from the report. Please also send this session as feedback so we can cover the case.

## Never

- edit ACLs by hand, change an owner, erase a deny, or replace child permissions recursively;
- run the script elevated, through UAC or `runas`;
- widen `-AllowRoot` to reach an ancestor, or repeat a denied or failed call.

Report in the user's language: analyzed paths, changes, verification, recovery commands, next step. Label an authorized unconfined run as such, not as a sandbox repair.
</skill_instructions>
</skill_content>

[assistant]
This requires running a script unconfined (danger-full-access) with approval. That's a permission escalation prompt. Given user just asked to read files, and read/glob works, maybe I don't need shell. But "读取文件信息" could include file sizes/timestamps. Hmm.

I think a reasonable move: gather project file structure via glob/read (works), then report that the shell is blocked and offer the repair. Asking the user to approve a permission change for a simple listing may be unnecessary friction. But the environment failure will block any build/run task later. Given the user only asked to read docs and file info, maybe just attempt listing via other means.

Actually file info could be obtained via glob results file paths, and reading key files. I'll also check git status indirectly? Not available.

Let me gather: miniprogram files, deploy files, releases files, android project files, backend README. Then summarize structure.

Let me do globs.

[assistant • glob]
{"pattern":"miniprogram/**/*"}

[assistant • glob]
{"pattern":"deploy/*"}

[tool • glob]
miniprogram\sitemap.json
miniprogram\app.json
miniprogram\app.wxss
miniprogram\pages\login\login.json
miniprogram\pages\index\index.json
miniprogram\pages\courses\courses.json
miniprogram\pages\courses\courses.wxml
miniprogram\pages\courses\courses.wxss
miniprogram\pages\courses\courses.js
miniprogram\pages\activities\activities.json
miniprogram\pages\activities\activities.wxml
miniprogram\pages\activities\activities.wxss
miniprogram\pages\activities\activities.js
miniprogram\pages\quiz\quiz.json
miniprogram\pages\quiz\quiz.wxss
miniprogram\pages\quiz\quiz.wxml
miniprogram\pages\quiz\quiz.js
miniprogram\pages\login\login.wxss
miniprogram\app.js
miniprogram\project.config.json
miniprogram\project.private.config.json
miniprogram\pages\login\login.wxml
miniprogram\pages\login\login.js
miniprogram\pages\slides\slides.json
miniprogram\utils\api.js
miniprogram\pages\index\index.js
miniprogram\pages\index\index.wxml
miniprogram\pages\index\index.wxss
miniprogram\pages\slides\slides.js
miniprogram\pages\slides\slides.wxml
miniprogram\pages\slides\slides.wxss

[tool • glob]
deploy\setup-server.sh
deploy\setup-ip-only.sh
deploy\logs.sh
deploy\fetch-logs.sh
deploy\parse-logs.sh
deploy\README.md

[assistant]
Also releases/, android key files, backend/README.md. Let me glob releases and read backend/README.md head. Also check root files list (from earlier grep): .env.example, metadata.json, tsconfig.json, makeup.html, index.html, vite.config.ts, auth-capture.log, pnpm-lock.yaml, package.json, capacitor.config.ts, .gitignore, ykt-logs.tar.gz, fullscreen.js, bddeploy.sh, and the md files.

Let me glob releases/* and root config reads.

[assistant • glob]
{"pattern":"releases/*"}

[assistant • glob]
{"pattern":"backend/*"}

[tool • glob]
releases\gongxiaohu-v1.0.apk
releases\gongxiaohu-v1.1.apk
releases\ykt-backend-scheme4.jar

[tool • glob]
backend\pom.xml
backend\.gitignore
backend\login-qrcode.jpg
backend\README.md
backend\auth.txt
backend\pc.js

[assistant]
Now, "读取文件信息" — maybe read file info: sizes and dates. Shell broken. Let me try the pwsh with danger-full-access? That requires justification and approval prompt. Hmm, the sandbox failure is a genuine blocker for file metadata.

Alternatively, on Windows I could use `Get-Item` via... only pwsh.

I think the best is to try once more with a `sandbox_permissions: danger-full-access` retry — the tool description says "narrowest wider sandbox mode for a one-shot retry of the exact operation the sandbox just denied". The denial is at workspace grant; full access avoids the grant. That would prompt the user for approval. That's reasonable and likely to succeed without needing ACL repair.

But will the user be annoyed? They asked to read file info; a prompt to allow a directory listing is heavy. Yet subsequent work (build/test) will need shell anyway. I'll do it with a clear justification in Chinese.

Actually wait — maybe workspace-write retry won't help; the failure is granting write ACL. danger-full-access should bypass. Let's try.

Before that, finish gathering with non-shell tools so even if denied I have content. Let me read backend/README.md, and key source summary? The user probably wants an orientation, not full code read.

Let me try the shell now with a listing + sizes + git status.

[assistant • pwsh]
{"command":"Get-ChildItem -Force | Sort-Object LastWriteTime -Descending | Select-Object LastWriteTime, @{n='Size';e={if($_.PSIsContainer){'<DIR>'}else{$_.Length}}}, Name | Format-Table -AutoSize; ''; git status --short; ''; git log --oneline -5","description":"List workspace files with sizes and git state","justification":"读取工作区文件信息需要执行目录列举；当前沙箱在创建 workspace 写权限时失败（Win32 5），因此这次只读列举需要完全访问权限才能运行。","sandbox_permissions":"danger-full-access"}

[tool • pwsh]

LastWriteTime       Size   Name                               
-------------       ----   ----                               
2026/11/25 12:34:02 <DIR>  .idea                              
2026/11/25 12:04:12 217    bddeploy.sh                        
2026/11/25 11:44:59 64931  技术说明.md                            
2026/11/25 11:27:22 998127 fullscreen.js                      
2026/11/25 11:19:11 <DIR>  dist                               
2026/11/25 11:19:07 4976   尝试策略.md                            
2026/11/25 11:06:36 <DIR>  .git                               
2026/11/25 10:44:22 <DIR>  releases                           
2026/11/25 10:43:17 <DIR>  android                            
2026/11/25 10:29:56 17164  正确实现链路.md                          
2026/11/24 18:32:49 727    日志获取.md                            
2026/11/24 18:24:02 <DIR>  deploy                             
2026/11/24 18:18:11 2275   ykt-logs.tar.gz                    
2026/11/24 18:16:41 912    .gitignore                         
2026/11/24 18:16:34 <DIR>  logs-backup                        
2026/11/24 17:53:55 <DIR>  backend                            
2026/11/24 13:49:17 292    capacitor.config.ts                
2026/11/24 13:48:49 1024   package.json                       
2026/11/24 13:48:49 <DIR>  node_modules                       
2026/11/24 13:48:49 110030 pnpm-lock.yaml                     
2026/11/24 13:33:53 147    .wbapp_nUHkTVcuk4od3SgflkzyeB.genie
2026/11/24 11:51:46 <DIR>  miniprogram                        
2026/11/24 9:21:30  <DIR>  .workbuddy                         
2026/11/24 8:49:53  <DIR>  .chrome-debug-profile              
2026/11/24 8:42:20  42     auth-capture.log                   
2026/11/23 22:40:13 5326   README.md                          
2026/11/23 14:16:48 6366   长江雨课堂-真实登录与抓包实测.md                 
2026/11/23 13:52:17 5342   雨课堂扫码协议-调研记录.md                    
2026/11/23 13:39:03 1156   vite.config.ts                     
2026/11/23 13:38:44 765    index.html                         
2026/11/23 12:57:50 750    makeup.html                        
2026/11/23 12:57:13 <DIR>  src                                
2026/11/23 12:19:04 565    tsconfig.json                      
2026/11/23 12:19:04 <DIR>  public                             
2026/11/23 12:19:04 487    metadata.json                      
2026/11/23 12:19:04 454    .env.example                       



 M android/app/src/main/java/site/jiclub/gongxiaohu/app/ClassroomEntryPlugin.java
 M android/app/src/main/java/site/jiclub/gongxiaohu/app/ClassroomWebViewActivity.java
 M backend/src/main/java/com/jiclub/gongxiaohu/controller/ContentController.java
 M backend/src/main/java/com/jiclub/gongxiaohu/controller/LessonController.java
 M backend/src/main/java/com/jiclub/gongxiaohu/gateway/RealYktGateway.java
 M backend/src/main/java/com/jiclub/gongxiaohu/gateway/YktGateway.java
 M backend/src/main/java/com/jiclub/gongxiaohu/gateway/YktInClassQuizApi.java
 M backend/src/main/java/com/jiclub/gongxiaohu/gateway/YktWebSocketLogin.java
 M backend/src/main/java/com/jiclub/gongxiaohu/model/Models.java
 M backend/src/main/java/com/jiclub/gongxiaohu/service/JoinLessonService.java
 M bddeploy.sh
 M deploy/README.md
 M releases/gongxiaohu-v1.0.apk
 M releases/ykt-backend-scheme4.jar
 M src/makeup/CourseCenter.tsx
 M src/makeup/LoginView.tsx
 M src/makeup/MakeupApp.tsx
 M src/makeup/api.ts
 M src/makeup/classroomEntry.ts
 M "\346\212\200\346\234\257\350\257\264\346\230\216.md"
 M "\346\255\243\347\241\256\345\256\236\347\216\260\351\223\276\350\267\257.md"
?? backend/src/main/java/com/jiclub/gongxiaohu/gateway/YktLiveLessonApi.java
?? backend/src/main/java/com/jiclub/gongxiaohu/model/CoursewareModels.java
?? backend/src/main/java/com/jiclub/gongxiaohu/service/FullCoursewareService.java
?? backend/src/test/java/com/jiclub/gongxiaohu/FullCoursewareTest.java
?? fullscreen.js
?? releases/gongxiaohu-v1.1.apk
?? "\345\260\235\350\257\225\347\255\226\347\225\245.md"

fb9b3ca 二维码修复
af3837b 二维码登录修复
229b3d6 更好的日志功能
899fa97 推导出正确链路并实现
4c9858b 完成apk


[assistant]
The danger-full-access worked. Now I have file info and git state.

I've read all 7 md docs (well, all with .md). And file info.

Now deliver a summary in Chinese. Keep it organized: docs read (7), what each says, then file info (structure, mtimes, git status, artifacts). Also note the shell sandbox issue (workspace grant Win32 5) — the first attempt failed but full-access worked; should mention so future operations need the same. Actually I should note that plain workspace-write shell is broken due to ACL, requiring full access.

The user's intent is likely onboarding/context-load before a task. Response should be concise but complete: current state of the project, latest doc updates (§13-15), pending items.

Key state as of docs:
- Latest doc §15: lesson_info_v2 500 root cause missing identity headers, fixed, jar repacked 11:44, 43 tests pass.
- 正确实现链路.md and 尝试策略.md: the real entry flow is lesson_info_v2 → userAuth → WS hello. checkin is sign-in.
- Tech doc §13: full PPT fetching (course-all + zip).
- §14: current classroom page aligned to history page; APK v1.1.
- releases: v1.0 apk, v1.1 apk, ykt-backend-scheme4.jar.

Uncommitted work is large (git status shows many M files, 6 new files).

Also worth noting: 技术说明.md was modified at 11:44 but 尝试策略.md at 11:19 and 正确实现链路.md at 10:29. The last doc section §15 corresponds to latest.

Let me write the answer in Chinese, with file links. Mention shell sandbox failure and that I need full access for commands.

Should I ask a question? The user only said read. I'll summarize and ask what's next, briefly.

File links: use relative paths with markdown links. E.g. [技术说明.md](技术说明.md). Per instructions link every file mention to full path relative to cwd.

Let me structure:

1. 已读文档（7个）— one line each with key content + latest sections.
2. 当前代码/文件状态 — structure tree summary, artifacts, git status.
3. 环境问题 — pwsh workspace grant failure; used full-access once.
4. 当前待验证/未完成事项 (from docs §7.5/§12/§15.4): 真实开课时验证 lesson_info_v2, 动态码课堂返回, APK 部署新 jar.

Keep it tight.

[assistant]
<acp tokens="1.2K" type="text" time="2026-11-25 12:44:26">m00071</acp>
已按顺序读完所有 `.md` 文档，并用 `glob`/`read` + 一次完全权限的 `pwsh` 拿到了文件与仓库状态。下面是汇总。

## 一、读到的 7 份文档

| 文档 | 内容要点 |
|---|---|
| [雨课堂扫码协议-调研记录.md](雨课堂扫码协议-调研记录.md) | 11-23 协议调研：`wss://*/wsapp/` 才是可用的扫码入口，`custom/auth/v1/auth/status` 实测 404；含四区域（standard/hetang/changjiang/huanghe）说明。 |
| [长江雨课堂-真实登录与抓包实测.md](长江雨课堂-真实登录与抓包实测.md) | 11-23 真机实测：学号 55120220、`changjiang.yuketang.cn` 扫码登录成功、7 门课课表、交题链路 `chapter→leaf_info→exercise_list→problem_apply` 及 payload 格式。 |
| [README.md](README.md) | 项目门面：React 19 + Vite 8 前端、Java 21 + Spring Boot 3.4.5 后端、mock/real 双模式、功能清单与接口表。 |
| [技术说明.md](技术说明.md) | 主交接文档（863 行，§1–§15）。核心：§4 接口实测表；§10–11 扫码登录两次故障修复（`subscribe_status` 误消费凭据、45 秒长轮询改 1.2 秒短轮询 + ticket 结果幂等缓存）；§12 APK 走原生 Cookie 注入官方课堂页；§13 全量 PPT 抓取（`/content/course-all` + `.zip`）；§14 当前课堂页对齐历史课堂页；§15 `lesson_info_v2` 500 的根因是缺 `university-id/uv-id/classroom-id` 身份头。 |
| [正确实现链路.md](正确实现链路.md) | 11-25 10:20 的关键纠正：官方学生端真实进课堂是 `GET /v/lesson/lesson_info_v2?lesson_id=&source=5` → 取 `data.userAuth` → WS `hello` 建会话；`checkin` 只是签到，50070 是动态码签到拒绝。旧推导（11-24）保留作对照。 |
| [尝试策略.md](尝试策略.md) | 上述纠正的分析过程：下载并逆向了官方课堂页 JS（fullscreen 3.3.241），逐条推翻旧判断；留下唯一未验证点——动态码课堂下 `lesson_info_v2` 返回 `code=0` 还是 50070。 |
| [日志获取.md](日志获取.md) | 服务器日志速查命令：audit.log 查学号、app.log 查错误、tar 打包回传。 |

## 二、文件与仓库现状

- 顶层：前端 `src/makeup/`（`api.ts`、`LoginView.tsx`、`MakeupApp.tsx`、`CourseCenter.tsx`、`classroomEntry.ts`）、`miniprogram/`（6 页原生小程序）、`backend/`（约 40 个 Java 类 + 11 个测试类）、`android/`（Capacitor 8）、`deploy/`（6 个脚本 + README）、`releases/`（`gongxiaohu-v1.0.apk`、`gongxiaohu-v1.1.apk`、`ykt-backend-scheme4.jar`）、`dist/`。
- 最近修改：`技术说明.md` 11:44:59 → `fullscreen.js` 11:27（998KB，官方课堂页 JS，新增未跟踪）→ `dist/` 11:19 → `尝试策略.md` 11:19 → `releases/` 10:44。
- Git：HEAD 为 `fb9b3ca 二维码修复`；工作区**大量未提交**——19 个已修改文件（含 `YktWebSocketLogin`、`JoinLessonService`、`MakeupApp.tsx`、两个文档、两个 APK/jar 产物），6 个未跟踪文件（`YktLiveLessonApi.java`、`CoursewareModels.java`、`FullCoursewareService.java`、`FullCoursewareTest.java`、`fullscreen.js`、`gongxiaohu-v1.1.apk`）。
- 后端配置：[application.yml](backend/src/main/resources/application.yml) 已是 `mode: real` + `region: changjiang`，`base-url: https://changjiang.yuketang.cn`。

## 三、一个环境问题

本会话的 `workspace-write` 沙箱在跑命令前就失败：`windows-acl workspace grant failed (Win32 5)`，任何 `pwsh` 都被拦在授权阶段（`read`/`glob`/`grep` 不受影响）。刚才那次列举是用「完全权限」一次性放行的。后续要跑 `mvn`/`pnpm`/`gradlew`，要么继续逐次批准完全权限，要么先修这个工作区的 Windows 文件权限（有现成诊断脚本）。需要的话我来处理。

## 四、文档里仍未闭环的事

1. 真实开课时验证 `lesson_info_v2` 是否能返回 `userAuth`；开了动态码的课堂返回 `code=0` 还是 50070（§7.5 / 正确实现链路 §5）。
2. §15 修复后新 jar 部署 + 重新扫码登录，再探测一次（文档写的是「已重打 jar，服务器尚未部署」）。
3. APK `v1.1` 需覆盖安装验证进课堂与全量 PPT。

接下来要做什么？（继续修链路、部署新 jar、还是提交这批未提交改动）