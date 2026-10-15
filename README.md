<h1>照面 · Zhaomian</h1>

<p>将 Android 手机用作前台人员监测与自动抓拍设备。项目使用 Kotlin、Jetpack Compose、CameraX 和 MediaPipe，本地完成识别，无需云端推理服务。</p>

<p><a href="https://github.com/RivellonLittlefairy/Zhaomian">GitHub 仓库</a> · 原名“过客哨”。更名后沿用原安装包 ID <code>com.example.passerwatch</code>，支持覆盖升级并保留原有 App 数据。新照片写入 <code>Pictures/Zhaomian</code>，旧 <code>Pictures/PasserWatch</code> 中的照片仍可查找。</p>

<h2>主要功能</h2>

<ul>
  <li>实时预览：持续分析相机画面，显示检测状态、实际 FPS 和推理耗时。</li>
  <li>条件抓拍：检测到可见人脸，或满足完整身体规则时触发；完整身体场景允许脸部模糊。</li>
  <li>持续监测：同一次有人经过只抓拍一次，确认人员离开后重新等待下一次经过。</li>
  <li>高清照片：检测使用较小图像，触发后通过独立拍照通道获取照片。</li>
  <li>相册查看：照片写入系统 Pictures/Zhaomian，App 内保留查看副本。</li>
  <li>镜头与缩放：根据设备支持情况选择镜头并调整缩放。</li>
  <li>检测设置：可调整目标分析帧率、置信度、连续确认帧数和离开确认时间。</li>
  <li>本地加速：模型优先尝试 GPU，支持在可恢复的 GPU 错误下回退 CPU。</li>
</ul>

<h2>运行流程</h2>

<pre><code>CameraX 摄像头
  ├─ Preview → Compose 中的 PreviewView
  ├─ ImageAnalysis → 本地模型 → 抓拍条件与事件去重
  └─ ImageCapture ← 满足抓拍条件
         ↓
      有界保存队列 → App 私有副本 + 系统相册
</code></pre>

<p>实时分析采用 KEEP_ONLY_LATEST：处理不及时就丢弃旧的待分析帧，优先处理最新画面。分析目标尺寸为 640×480，拍照目标尺寸为 1920×1440，实际输出由设备能力与 CameraX 协商确定。</p>

<h2>技术栈</h2>

<ul>
  <li>Kotlin 2.0.21、Jetpack Compose、Material 3。</li>
  <li>ViewModel、StateFlow、Lifecycle、Kotlin Coroutines。</li>
  <li>CameraX 1.4.2：预览、图像分析、拍照和生命周期绑定。</li>
  <li>MediaPipe Tasks Vision 0.10.21：BlazeFace、EfficientDet-Lite0、Pose Landmarker Lite。</li>
  <li>MediaStore：向系统相册发布照片。</li>
</ul>

<h2>构建与运行</h2>

<ol>
  <li>使用支持 AGP 8.13.2 的 Android Studio 打开项目。</li>
  <li>配置 Gradle JDK，建议使用 JDK 21；安装 Android SDK 36。</li>
  <li>等待 Gradle 同步完成，连接 Android 7.0（API 24）及以上、具备摄像头的设备。</li>
  <li>运行 app 模块，按提示授予相机权限。</li>
</ol>

<p>Windows 命令行构建：</p>

<pre><code>.\gradlew.bat :app:assembleDebug</code></pre>

<p>APK 输出：<code>app/build/outputs/apk/debug/app-debug.apk</code>。项目已包含模型文件，运行时无需下载模型。首次同步依赖仍需要网络。</p>

<h2>使用方式</h2>

<ol>
  <li>打开实时页面，选择合适的镜头、缩放和摆放角度。</li>
  <li>点击“开始检测”，保持 App 在前台。</li>
  <li>人员满足抓拍条件后自动拍照；确认人员离开后自动重新布防。</li>
  <li>在 App 相册页或系统 Pictures/Zhaomian 中查看照片。</li>
  <li>需要调整规则时先停止检测，再修改设置。</li>
</ol>

<p>默认目标分析帧率为 20 FPS，连续确认 2 帧，离开确认时间为 2 秒。目标帧率不等于实际帧率，实际速度取决于手机性能、模型后端、温度和相机输出。</p>

<h2>代码入口</h2>

<ul>
  <li><code>MainActivity.kt</code>：Compose 页面入口。</li>
  <li><code>ui/MonitorScreen.kt</code>：页面协调、权限、生命周期和相机接入。</li>
  <li><code>ui/MonitorViewModel.kt</code>：监测会话与页面状态。</li>
  <li><code>camera/CameraController.kt</code>：CameraX 绑定、拍照和镜头缩放。</li>
  <li><code>camera/FrameAnalyzer.kt</code>：分析帧接收、采样和推理调度。</li>
  <li><code>detection/PersonDetector.kt</code>：人脸、人员和姿态模型调用。</li>
  <li><code>capture/CaptureGate.kt</code>：确认、去重和重新布防。</li>
  <li><code>capture/</code>：保存队列、文件写入和相册发布。</li>
</ul>

<p>上述 Kotlin 路径均相对于 <code>app/src/main/java/com/example/passerwatch/</code>。</p>

<h2>权限与使用范围</h2>

<p>相机权限用于预览、分析和拍照。Android 9 及以下写入系统相册还需要存储写入权限，Android 10 及以上通过 MediaStore 保存自有照片。Manifest 移除了 INTERNET 权限，识别在本机进行。</p>

<p>当前面向前台使用，不提供后台或锁屏持续监测保障。GPU 和多镜头能力依设备而异；完整身体条件是基于模型关键点的规则判断，遮挡、弱光和拍摄角度可能造成漏检或误判。触发后的照片与检测帧存在时间差。</p>

<h2>项目文档</h2>

<ul>
  <li><a href="docs/plans/index.html">实施计划</a></li>
  <li><a href="docs/verification/使用说明.html">使用说明</a></li>
  <li><a href="docs/verification/模型与依赖.html">模型来源、版本与许可说明</a></li>
</ul>

<p>docs 中的详细文档采用 HTML，可下载后使用浏览器打开。模型及测试样本的来源记录见上述文档，分发时需分别遵守对应资产的许可要求。</p>
