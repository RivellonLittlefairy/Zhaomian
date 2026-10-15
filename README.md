<h1>照面 · Zhaomian</h1>

<p>让 Android 手机持续观察画面，在有人经过时自动拍下一张照片。识别在手机本地完成。</p>

<p><a href="https://github.com/RivellonLittlefairy/Zhaomian/releases/tag/v1.0.0">下载 APK</a></p>

<h2>功能</h2>

<ul>
  <li>检测到可见人脸或完整身体时自动抓拍，人员离开后等待下一次经过。</li>
  <li>双指缩放，根据设备能力自动切换镜头。</li>
  <li>独立高清拍照，照片可在 App 相册和系统相册中查看。</li>
  <li>日夜主题切换，实时显示检测状态和处理帧率。</li>
  <li>可调整检测帧率、置信度和抓拍确认条件。</li>
  <li>优先使用 GPU 加速，不可用时自动回退 CPU。</li>
</ul>

<h2>使用</h2>

<ol>
  <li>打开应用，允许相机权限。</li>
  <li>摆好手机，通过双指缩放调整取景。</li>
  <li>点击“开始检测”，保持应用在前台。</li>
  <li>在相册页查看抓拍照片。</li>
</ol>

<p>支持 Android 7.0 及以上。Android 9 及以下保存到系统相册需要存储权限。当前仅支持前台监测。</p>

<h2>构建</h2>

<p>使用 Android Studio 打开项目，配置 JDK 21 和 Android SDK 36，然后运行 app 模块。首次同步依赖需要网络，识别模型已随项目提供。</p>

<pre><code>.\gradlew.bat :app:assembleDebug</code></pre>

<p>APK 输出：<code>app/build/outputs/apk/debug/app-debug.apk</code>。</p>

<h2>技术栈</h2>

<p>Kotlin · Jetpack Compose · Material 3 · CameraX · MediaPipe · BlurView</p>

<h2>文档</h2>

<ul>
  <li><a href="docs/verification/使用说明.html">使用说明</a></li>
  <li><a href="docs/verification/模型与依赖.html">模型来源与许可</a></li>
</ul>
