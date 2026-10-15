---
name: Sentient Sentry Tactical
colors:
  surface: '#111415'
  surface-dim: '#111415'
  surface-bright: '#373a3b'
  surface-container-lowest: '#0c0f10'
  surface-container-low: '#191c1d'
  surface-container: '#1d2021'
  surface-container-high: '#272a2b'
  surface-container-highest: '#323536'
  on-surface: '#e1e3e4'
  on-surface-variant: '#bacbb9'
  inverse-surface: '#e1e3e4'
  inverse-on-surface: '#2e3132'
  outline: '#859585'
  outline-variant: '#3b4a3d'
  surface-tint: '#00e474'
  primary: '#6dff9a'
  on-primary: '#003918'
  primary-container: '#00e575'
  on-primary-container: '#00602d'
  inverse-primary: '#006d34'
  secondary: '#bdf4ff'
  on-secondary: '#00363d'
  secondary-container: '#00e3fd'
  on-secondary-container: '#00616d'
  tertiary: '#ffdfaf'
  on-tertiary: '#432c00'
  tertiary-container: '#ffbb3d'
  on-tertiary-container: '#704c00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#62ff96'
  primary-fixed-dim: '#00e474'
  on-primary-fixed: '#00210b'
  on-primary-fixed-variant: '#005226'
  secondary-fixed: '#9cf0ff'
  secondary-fixed-dim: '#00daf3'
  on-secondary-fixed: '#001f24'
  on-secondary-fixed-variant: '#004f58'
  tertiary-fixed: '#ffdeac'
  tertiary-fixed-dim: '#ffba38'
  on-tertiary-fixed: '#281900'
  on-tertiary-fixed-variant: '#604100'
  background: '#111415'
  on-background: '#e1e3e4'
  surface-variant: '#323536'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Space Grotesk
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.04em
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.06em
  telemetry-data:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 0.75rem
  gutter-tablet: 1rem
  margin: 1rem
  margin-tablet: 1.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 1.75rem
---

## Brand & Style

The design system is constructed for long-lifecycle, edge-compute hardware monitoring where consumer devices transform into autonomous sentries. The aesthetic marries utilitarian industrial telemetry with high-legibility avionics HUD paradigms. It prioritizes zero-distraction focus, visual clarity under extreme ambient lighting variations, and strict thermal/energy conservation for continuous 24/7 runtimes on repurposed mobile hardware.

### Design Principles
- **Hardware-First Utility**: Interfaces reflect the rugged nature of standalone diagnostic instruments. Visual ornamentation is eliminated; every outline, status marker, and metric exists to convey actionable edge-state data.
- **Thermal & OLED Preservation**: Deep pure blacks (`#000000`) serve as the foundation to minimize panel power consumption and mitigate burn-in on stationary displays. Low-intensity structural gray frameworks guide the eye without overwhelming the camera viewfinder.
- **Mission Critical Semantics**: System state is communicated instantly via standardized functional color signals—active patrol, cold standby, anomaly tripwires, and computer vision classification frames.
- **Instrument Tactility**: Physical toggles, mechanical keypads, and sensor HUD telemetry inspire button states, edge tags, and metadata readouts.

## Colors

The color palette is built strictly for high-contrast dark environments, preserving OLED efficiency while giving distinct meaning to system telemetry and target acquisition data.

### Functional Color Assignments
- **Primary (`#00E575` — Sentinel Emerald)**: Designates nominal surveillance operation, healthy neural execution loops, active on-device monitoring, and continuous arming.
- **Secondary (`#00E5FF` — Telemetry Cyan)**: Reserved strictly for computer vision metadata, MediaPipe 2D bounding boxes, inference confidence tags, and hardware sensor telemetry (thermal, frame timing, battery current).
- **Tertiary (`#FFB300` — Standby Amber)**: Used for standby, arming countdowns, sensor calibration, and suppressed threshold warnings.
- **Alert / Trigger (`#FF3B30` — Intrusion Crimson)**: Indicates tripwire breaches, recognized object alerts, storage bottlenecks, and hardware failovers.
- **Neutral Canvas (`#000000` / `#0A0D0E` — Pitch Void & Carbon Slate)**: Pure black serves as the primary canvas for maximum panel efficiency. Slate tones (`#12171A`, `#1A2126`, `#252E35`) build the panel tier architecture.
- **Data Neutral / Muted Grid (`#4A555E`, `#8B97A0`, `#E1E7EC`)**: Fixed coordinate ticks, camera reticles, muted secondary labels, and high-readout alphanumeric readouts.

## Typography

The typographic hierarchy establishes clear structural separation between technical diagnostics, camera HUD instrumentation, and operational status controls:

- **Headlines (Space Grotesk)**: Chosen for its mechanical, geometric posture that grounds top-level screens, operational states, and major card headers.
- **Body Text (Hanken Grotesk)**: Provides neutral, clear legibility for logs, alert summaries, configuration settings, and local security descriptors.
- **Technical Readouts & Telemetry (JetBrains Mono)**: Monospaced alignment is strictly enforced across all live metric counters (FPS, inference ms, battery level, storage remaining), coordinate readouts, bounding box tags, and timecode stamps. This prevents tabular jitter and layout shift during high-frequency sensor updates.

## Layout & Spacing

Layouts conform to a high-density, screen-maximizing grid system engineered for touch interfaces, continuous monitoring displays, and split-screen landscape orientations.

### Viewport Adaptation & Grids
- **Mobile Portrait (Default Sentinel Handheld)**: 4-column fluid layout with `0.75rem` gutters and `1rem` safe-area outer padding. UI controls cluster along the bottom edge for easy thumb reach, while live diagnostics and HUD reticles live in the upper focal zone.
- **Mobile Landscape (Mounted Camera Viewfinder)**: Split 8-column layout. The live camera stream occupies a fixed 6-column center/left stage, flanked by a 2-column vertical telemetry dock and manual override strip.
- **Tablet / Large Display**: 12-column fixed grid with `1rem` gutters and `1.5rem` margins. Multiview surveillance splits the viewfinder feed and detailed inference log streams side by side.

### Spacing Rhythm
Spacing relies on a compact 4px base module, using `space-xs` (4px) and `space-sm` (8px) for tightly coupled telemetry chips, and `space-md` (12px) to `space-lg` (20px) for container boundaries and touch targets. This maximizes visible camera real estate on older screens.

## Elevation & Depth

Visual depth is achieved entirely without heavy blur effects or GPU-intensive drop shadows, ensuring stable frame rates and thermal efficiency on low-spec hardware.

### Tonal Stratification & Borders
- **Level 0 (Camera Bed / Ground Void)**: Deep `#000000`. Used for raw camera feed underlays and full-canvas blacked background modes.
- **Level 1 (Dock & Deck Shells)**: Surface `#0D1114` with a 1px crisp outline of `#1E262C`. Hosts persistent status bars, lower control pods, and settings sheets.
- **Level 2 (Telemetry Blocks & Event Tiles)**: Surface `#151B20` framed with 1px border `#28333B`. Hosts individual event logs, detection triggers, and metric modules.
- **Level 3 (Tactile Control Surfaces & Critical Modals)**: Surface `#1D252C` with border `#384550`. Active touch surfaces and floating sensor status overlays.

### Tactical Border Accents
Elements signal alert readiness through direct, 1px high-contrast structural borders:
- Active Sentinel state frames elements in `rgba(0, 229, 117, 0.4)`.
- Triggered Intrusion frames flash sharply in solid `#FF3B30`.
- Object bounding overlays render with crisp, non-blurred 1.5px `#00E5FF` wireframes accompanied by high-contrast coordinate corner crosshairs.

## Shapes

The design system embraces an architectural, semi-machined corner geometry (`roundedness: 1` — 4px baseline, 8px for containers). 

- **Containers & Deck Sheets**: Standardized at `0.5rem` (8px) corner radius, preserving a rugged, appliance-like appearance.
- **HUD Badges, Chips & Buttons**: Clamped at `0.25rem` (4px) to retain technical precision.
- **Viewfinder Target Brackets**: 0px radius corners with right-angle crosshairs, emphasizing precise computer vision tracking.
- **Pill Shapes**: Strictly reserved for active record/sentry status indicators to signify physical hardware LED lamps.

## Components

### Buttons & Tactical Triggers
- **Primary Sentinel Trigger (ARM / DISARM)**: Height 52px. Full width or large footprint. Dark carbon surface (`#12171A`) surrounded by a 1.5px solid border in `#00E575` (when disarmed/ready) or `#FF3B30` (when hot/active). Type: `Space Grotesk` uppercase bold, with a leading 8px glowing status dot. Active tap shifts background to 20% accent tint with zero elevation displacement.
- **Secondary Actions (Calibrate, Flush Cache, Flip Lens)**: Height 40px. Surface `#1A2126`, border 1px `#2D3942`, label in `Hanken Grotesk` 14px medium `#E1E7EC`.

### HUD Overlay & Bounding Boxes
- **CV Object Bounding Frame**: Unfilled bounding rectangle with 1.5px stroke `#00E5FF`. Four right-angle corner brackets extend 6px outward.
- **Detection Pill Tag**: Anchored to the top-left of the bounding box. Solid `#00E5FF` fill with pure `#000000` text rendered in `JetBrains Mono` 10px bold: `[PERSON 94%]`.
- **Viewfinder Reticle**: Center-screen fine hair-cross with 1px dashed grid markings in `rgba(255, 255, 255, 0.2)`.

### Status Chips & Telemetry Tags
- **Metric Pill**: Height 22px. Dark slate surface (`#12171A`), 1px border (`#252E35`). Displays icon, label in muted gray (`#8B97A0`), and real-time value in monospace (`#00E5FF` or `#00E575`). Example: `FPS: 29.8`, `CPU: 41°C`, `BATT: 98% AC`.
- **State Badges**: Standby (`#FFB300`), Guarding (`#00E575`), Alert (`#FF3B30`).

### Cards & Event Lists
- **Incident Event Card**: Surface `#0D1114`, 1px border `#1E262C`. Left-hand 3px vertical accent stripe reflecting the event severity. Contains thumbnail frame with overlaid bounding rect, timestamp in `JetBrains Mono` 12px, label (e.g., `PERSON DETECTED`), and duration metadata.
- **Card States**: Normal, Highlighted on playback selection (border changes to 1px `#00E5FF`), and Urgent Breach (border pulses `#FF3B30`).

### Form Controls & Threshold Inputs
- **Detection Sensitivity Slider**: Track height 4px, background `#1E262C`, active track `#00E575`. Thumb is a sharp 14x14px square (`#FFFFFF`) with a 1px border (`#000000`). Numeric value badge dynamically updates above the thumb in real-time.
- **Checkboxes & Segmented Toggles**: Sharp 18x18px squares with 2px radius. Checked state displays `#00E575` fill with black checkmark icon. Unchecked displays `#151B20` with `#28333B` border.
- **Input Fields (RTSP Stream / Webhook URL)**: Surface `#080A0B`, border 1px `#252E35`, focused state border 1.5px `#00E5FF`. Text strictly monospaced with monospace placeholder text in `#4A555E`.

### Telemetry Dock (Landscape View)
- Vertical rail pinned to the right edge. Stacks battery thermal throttling alerts, continuous run-time clocks, storage gauge bars, and camera stream diagnostics in a strict, high-density terminal layout.