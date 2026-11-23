# Readability and accessibility

Version 1.2.0 focuses on making the existing clock easier to adjust and read.

## Controls

- Main action buttons have a minimum height of 60 dp. Slider step buttons are at least 56 dp wide, and slider rows are 64 dp high.
- Text uses `sp` units. Choices stack vertically when system font scale is large so their labels can wrap.
- Most control heights grow with their content. Tests exercise system font scale at 200% on Android 12 and Android 15.
- Plus and minus buttons provide an alternative to dragging a slider. The color picker also offers RGB controls and hex input as alternatives to its two-dimensional color square.
- The full-screen clock has a visible exit button, in addition to its existing double-tap gesture.

## Contrast

The UI uses contrasting text colors, including black or white text selected for colored buttons. Optional clock contrast correction targets 4.5:1 for text and 3:1 for hands and marks against the selected clock face. The high-contrast option makes the clock face opaque.

With a transparent widget, the real background is the user's wallpaper. Its contrast cannot be guaranteed by the app; use the opaque high-contrast option for a predictable background. The logo retains its original design.

## Screen readers

Settings have descriptive labels, headings, selected states and slider values. The custom clock exposes a text description containing its current time, date and custom text. Decorative duplicate text is excluded from focus order. TalkBack should still be checked on the intended device.

## Keeping settings steady

Normal setting changes update the existing views and coalesce preview updates to animation frames. The app does not replace the settings page when a color, theme or size changes. Widget image updates are debounced and run on a background worker. Text editing, focus and scroll position are covered by regression checks.

These are documented implementation targets and automated checks, not a claim of independent accessibility certification.

References: [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps) and [WCAG contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

---

## راهنمای کوتاه فارسی

برای شروع، «تنظیم پیشنهادی برای خوانایی بیشتر» را بزن. بعد می‌توانی اندازهٔ هر قسمت را جداگانه تنظیم کنی. اگر کار با نوار لغزنده راحت نیست، از دکمه‌های + و − استفاده کن.

برای رنگ دلخواه، بخش مورد نظر ساعت را انتخاب کن و جعبهٔ رنگ را باز کن. رنگ‌های آماده، انتخاب دقیق رنگ و کنترل‌های قرمز/سبز/آبی در دسترس‌اند. وقتی حفظ کنتراست روشن است، رنگ روی ساعت ممکن است برای خواناتر شدن کمی تغییر کند.

برای ویجت روی تصویر زمینهٔ شلوغ، پس‌زمینهٔ کامل و پرکنتراست را فعال کن. تغییر اندازهٔ متن گوشی روی نوشته‌های تنظیمات هم اعمال می‌شود. هنگام باز بودن صفحه‌کلید، پیش‌نمایش موقتاً کنار می‌رود تا محل نوشتن در دسترس بماند؛ با بسته‌شدن صفحه‌کلید همان پیش‌نمایش برمی‌گردد.
