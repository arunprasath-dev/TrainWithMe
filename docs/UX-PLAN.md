# TrainWithMe — UX, visual design & motion improvement plan

**Review date:** 2026-10-27  
**Scope:** Android Compose app in this repository  
**Approach:** improve confidence, clarity and workout completion first; add retention features only when they support healthy training habits. No dark patterns, guilt copy, fake scarcity, or engagement-for-engagement's-sake.

## Executive review

TrainWithMe already has a strong product foundation: an offline-first guided workout loop, a coherent ember-and-neutral palette, light/dark themes, reusable Compose components, readable type, explicit exercise guidance, a prominent workout action, and a thoughtful domain model. The visual language is consistent and the app is more than a wireframe.

The next quality step is to make it feel **purposeful and alive without becoming noisy**. The highest-impact opportunities observed in the current implementation are:

1. **Screen changes and onboarding steps are abrupt.** The app shell only fades between destinations; individual questionnaire steps switch immediately.
2. **The dashboard is visually static.** Its hierarchy is sound, but the primary action, weekly progress and data cards all arrive with equal visual weight. The adherence ring and quick weigh-in disclosure snap to their new state.
3. **Surface hierarchy is subtle.** Most cards have the same treatment. A clear “today” hero, restrained tonal depth and a low-contrast background wash can give the eye a better starting point.
4. **Primary destinations are not persistently reachable.** Plan is discovered in dashboard content and Progress is a top-bar shortcut; test whether a small Today / Plan / Progress navigation bar reduces navigation effort without stealing space from the workout player.
5. **Workout state changes are the most important interaction and deserve especially clear feedback.** Continue to audit exercise/set transitions, timer completion, logging feedback, video loading/failure, and one-handed use on device.
6. **The product is personal and habit-forming by nature, but feedback should celebrate consistency without punishing missed days.** Existing streaks can be useful, but should not imply failure or encourage unsafe make-up sessions.
7. **Some quality must be verified on real devices.** Reduced-motion settings, TalkBack traversal, large font, contrast, haptics, frame smoothness, battery use and video behaviour cannot be signed off from source inspection alone.

This is an implementation plan, not a claim that the app has a feature simply because it is on the roadmap. Offline-first and privacy constraints remain product requirements.

## UX principles and measurable outcomes

- **One obvious next action:** a new or returning user should quickly know whether to start, continue, recover, or build a plan.
- **Motion explains change:** transitions indicate hierarchy or changed state; decorative animation never delays a workout action.
- **Premium, not loud:** use space, typography, shape, colour and a small number of soft tonal layers; avoid constant movement and stacked shadows.
- **Trustworthy coaching:** show why a prescription exists, distinguish estimates from measurements, make correction and recovery easy.
- **Inclusive by default:** scalable text, accessible names, strong focus order, adequate touch targets, reduced-motion support and no colour-only status.
- **Healthy retention:** celebrate completed sessions and returning after a break; never shame a rest day or missed workout.
- **Private and resilient:** no analytics or network requirement; local state must survive rotation, interruption and process recreation.

### Success criteria (baseline, then validate with user testing)

| Outcome | Proposed acceptance target |
|---|---|
| Find today's action | 5/5 first-time test participants identify the next action in ≤5 seconds |
| Begin training | From a loaded dashboard, workout entry is reachable in no more than 2 deliberate taps |
| Log a working set | User can identify and complete the primary set action one-handed; no required control is hidden at 200% font scale |
| Understand progress | Participants can distinguish planned, completed and recovery states without relying on colour alone |
| Motion quality | No blocking animation; standard transitions stay under 300 ms; state feedback is immediate |
| Accessibility | No critical TalkBack focus/label defects; controls remain usable at 200% font scale and with system animation scales disabled |
| Reliability | Rotation/process recreation during a session preserves the active exercise, logged sets and timer semantics |

These are proposed validation gates, not current measured results.

## End-to-end roadmap

### Phase 0 — Audit, baseline and tokens (this pass)

- [x] Inspect visual system, app shell, dashboard, onboarding, workout player, existing plan and accessibility notes.
- [x] Capture app-specific findings and a prioritized implementation/verification plan here.
- [x] Add restrained dashboard depth and establish more differentiated card elevation.
- [x] Make the dashboard's key arrival and progress interactions feel responsive.
- [x] Add directional navigation and onboarding transitions.
- [ ] On a device, record baseline screenshots in light/dark, phone/tablet if supported, 1.0×/1.3×/2.0× font, TalkBack, and system animations off.

### Phase 1 — Core visual system and motion (in progress)

- [x] **Navigation:** short fade + small directional slide for forward/back stack changes; never use a large page-swipe that suggests a pager.
- [x] **Onboarding:** animate question-step changes while keeping progress and action controls stable.
- [x] **Dashboard:** stagger the primary workout, week strip and supporting cards with a short one-time entrance; use a low-contrast brand wash, a tonal primary card, and a softly animated adherence ring.
- [x] **Disclosure:** animate nutrition explanation and weigh-in form expand/collapse.
- [x] **Surfaces:** introduce restrained elevation and pressed elevation for reusable cards.
- [x] **Navigation:** add a compact Today / Plan / Progress destination bar; hide it on onboarding, settings and active workouts to preserve focus and vertical room.
- [x] **Workout feedback:** smoothly reflect logged-set state and rest-bar progress; expose set number plus current/completed/upcoming state to accessibility services.
- [x] **Rest timer resilience and alert:** persist an absolute end-time across process recreation; a settings-controlled notification-stream beep and system haptic fire only when rest naturally expires.
- [ ] **Workout player QA:** validate exercise changes, set completion, rest countdown and summary transition on device. Keep the set action immediate and timer accurate; avoid animation-driven timers.
- [x] **Media loading/error:** show a buffering indicator, report player failure and keep the technique fallback visible. Existing dashboard/progress empty states stay actionable.
- [ ] **Loading/empty/error QA:** verify all failure paths on device; replace any remaining indefinite spinner with retry/recovery copy.
- [ ] **Reduced motion:** inspect all transitions with Android “Remove animations” / animator duration scale set to zero; provide a central motion policy if needed, ensuring content is never gated on animation completion.

### Phase 2 — Feedback, gesture and accessibility polish

- [x] Add non-blocking visual recognition for logged sets and meaningful session milestones; milestones can be dismissed and do not require sharing.
- [x] Rest-end haptic/beep is optional and controlled by the existing workout-alert setting; Android's haptic system settings remain authoritative.
- [ ] Audit alert intensity on device and reduce/remove it if intrusive.
- [x] Preserve Material pressed/focus states, accessible selection state, and set-number/current/completed/upcoming semantics.
- [x] Add textual semantics for trend charts, weekly volume charts and the exercise demo surface; timer countdown remains visible as text.
- [ ] Check keyboard/IME behaviour, focus restoration, back handling and confirmation-dialog focus on each major flow.
- [ ] Check contrast in both themes, particularly semantic colours, disabled controls, tiny labels and tinted cards.
- [ ] Verify large font, display cutouts, edge-to-edge insets, landscape and compact screens. Permit scrolling rather than clipping.
- [ ] Review color-blind and non-colour status cues (icons, text, shape) for day status, errors and PRs.

### Phase 3 — Return value and ethical retention

- [x] Add a compassionate return-after-break state (shown after a 14+ day gap) without reset/shame language.
- [x] Celebrate session-count milestones in the recap with dismissible, non-blocking copy; existing PRs continue to be highlighted.
- [x] Keep weekly reflection focused on sessions/adherence/streak and not only body weight; the progress view presents weight, volume and records as separate measures.
- [x] Add an explicit workout recap share action. The shared text contains session title, duration, set/repetition totals and PR names, but no body metrics or lift loads.
- [x] Add an offline home-screen widget for the next planned session; it exposes no body metrics and only opens the app when tapped.
- [x] Keep reminders opt-in and user-scheduled; one reminder is scheduled only for training days. Reminder time and enable/disable remain available in settings.

### Phase 4 — Content and advanced capability

- [ ] Replace schematic clips with licensed, per-exercise content; add captions, playback speed, reliable loading/error fallback, and an explicit low-data/offline strategy.
- [ ] Evaluate Health Connect as opt-in, scoped read/write permission with clear provenance and local-only fallback.
- [ ] Localize the interface (Tamil/Hindi candidates) only after string externalization and layout expansion testing.
- [ ] Consider adaptive mesocycle changes only with explainable recommendations, user review and a safe undo path.
- [ ] Consider Wear OS only after phone workout flow and persistence are proven robust.

### Phase 5 — QA, performance and release

- [x] Add initial Compose UI smoke tests for the destination bar, dashboard no-plan action, reusable primary button and accessible weight-chart summary.
- [ ] Extend Compose coverage to onboarding progression, session set logging/rest states, summary milestones/share and reduced-motion semantics.
- [ ] Capture screenshot/golden coverage for dashboard, player, progress and onboarding in light/dark.
- [ ] Run on representative low/mid/high devices; inspect jank during chart drawing, navigation and video playback; profile before adding complex effects.
- [ ] Test offline, process death, rotation, permission denial, notification quiet hours and backup/export/delete paths.
- [ ] Run accessibility scanner + manual TalkBack, switch access where possible, 200% font scale and reduced motion.
- [ ] Release gate: all implemented checklist items pass; no critical accessibility defects; no animation blocks input; no privacy regression or newly required network permission.

## Visual system direction

- **Colour:** retain Ember as the focused action/highlight, neutral surfaces for reading and distinct semantic colours for status. Avoid using the accent on every component.
- **Depth:** distinguish the primary action from supporting material with one tonal hero surface and at most subtle elevation; avoid shadow-on-shadow.
- **Background:** use only static or very slow, low-contrast ambient colour. No perpetual particles, flashing or moving gradients behind exercise instructions.
- **Type:** keep the current hierarchy; reserve display/headline styles for meaningful moments and allow full system font scaling.
- **Shape:** retain the rounded, friendly cards, but use more compact shapes for chips/controls so cards do not all feel like large pills.
- **Motion:** transitions generally 160–260 ms, entrances one-shot and staggered by a small amount, state animation 180–650 ms depending on semantic meaning. Timers use elapsed time, not frame counts.
- **Sound/haptics:** optional, user-controlled, quiet by default; never the only feedback channel.

## Prioritization

**P0 — trust/use:** workout set logging and timer resilience, onboarding clarity, dashboard next action, empty/error/loading states, TalkBack and scale.  
**P1 — polish:** shared motion language, visual hierarchy, meaningful completion feedback, chart explanations, screenshots and UI tests.  
**P2 — return value:** compassionate reminders, milestones, reflection and widget.  
**P3 — expansion:** licensed video pipeline, Health Connect, localization, adaptive coaching, Wear OS.

Work through P0 and P1 before adding a stream of new animation or social mechanics. Avoid a high-motion treatment as a substitute for unfinished video content or missing empty states.

## Implementation tracker

| Item | Status | Evidence / follow-up |
|---|---|---|
| UX audit and phases documented | Done | This document |
| Global forward/back screen transitions + destination navigation | Implemented | `app/ui/TrainWithMeApp.kt`; Today / Plan / Progress bar hides during focus flows |
| Onboarding step transition | Implemented | `feature/onboarding/ui/OnboardingScreen.kt` |
| Dashboard stagger, tonal primary card, background wash and compassionate return note | Implemented | `feature/plan/ui/DashboardScreen.kt`, `feature/plan/component/PlanComponents.kt` |
| Adherence ring and weekly reflection | Implemented | `core/designsystem/component/Charts.kt`, dashboard summary |
| Workout set, rest-timer feedback and timer recovery | Implemented | `SessionPlayerScreen.kt`, `WorkoutViewModel.kt`: accessible set status, smoothed timer, system-respecting beep/haptic toggle, SavedStateHandle rest recovery |
| Dismissible session milestones and private-by-default share recap | Implemented | `WorkoutViewModel.kt`, `SessionSummaryScreen.kt` |
| Expand/collapse motion and card elevation | Implemented | Dashboard and `core/designsystem/component/Components.kt` |
| Video buffering/error fallback and chart accessibility summaries | Implemented | `VideoPlayer.kt`, `Charts.kt` |
| Offline home-screen next-workout widget | Implemented | `app/widget/NextWorkoutWidgetProvider.kt` and widget resources |
| Compose smoke tests | Added, unexecuted | Destination bar, no-plan action, primary button and weight-chart semantics |
| Build / visual / accessibility device verification | Pending | No Java runtime was available to execute Gradle in this pass; run Gradle CI and device QA |
| Licensed per-exercise footage, Health Connect, full Tamil/Hindi localization, Wear OS, screenshot golden suite | Blocked / deferred | Requires content rights, dependency/toolchain and permission design, translation and real-device validation; do not claim as shipped |

## Items that require external inputs or device validation

The repository-side improvements above are implemented, but “finish everything” cannot responsibly mean shipping fabricated or unverified integrations:

- **Licensed exercise clips and captions:** requires a rights-cleared source library and reviewed coaching/caption text. The existing generated demo assets remain placeholders; the player now exposes load/error fallback.
- **Health Connect:** requires an approved set of data types, user-facing consent/provenance copy, Health Connect SDK resolution and API-level/device testing. Do not silently import health/body data.
- **Tamil/Hindi localization:** the app has substantial hard-coded UI copy. Full localization needs systematic string externalization and human-reviewed translations plus layout testing; a handful of translated strings would be misleading.
- **Wear OS:** is a separate product surface and requires a companion module, pairing/data-transfer decisions and wearable test devices.
- **Visual golden and manual accessibility QA:** instrumentation smoke tests were added, but no Java runtime/emulator was available in this workspace to run them, capture screenshots, inspect TalkBack, validate reduced-motion behaviour or profile jank. These remain release gates, not presumed passes.

## Definition of done for each UX change

1. The change improves an identified user task, not just novelty.
2. Content and actions remain available if animation is disabled or interrupted.
3. No critical information is communicated by motion or colour alone.
4. It works in light/dark, large text, TalkBack and on low-end hardware.
5. The code has a preview or test where practical and a device validation note.
6. It preserves offline-first behaviour, private-by-default data and user-controlled reminders.
