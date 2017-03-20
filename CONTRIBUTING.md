# Contributing to Android Slideshow

So you want to give us a hand to fix some things up? That's awesome! Thanks a lot!

Here's everything you need to know to contribute to Android Slideshow.

Please read the commit recommendations at the bottom of this document before submitting a pull request.

## Found a bug?

Do a quick search through the [issues list][1] to make sure it hasn't already been listed.
If not, hit the [new issue](https://github.com/ScreamingHawk/android-slideshow/issues/new) button and add some details.

Please do this even if you plan to fix it yourself.
It's nice to track these things.

I'll personally respond to all issues.

## Want to fix something?

Great!

1. Download the source code using `git clone https://github.com/ScreamingHawk/android-slideshow.git`.
2. Open the project in [Android Studio](https://developer.android.com/studio/index.html) or another Android development environment.
3. Make your changes.
4. Click run.
5. **Ensure each commit references the issue / feature number** with `#<Number>`.
6. Submit a pull request via Github.

Look forward to your fix being included!

## Can I translate the app?

Yes please!

1. Download the source code using `git clone https://github.com/ScreamingHawk/android-slideshow.git`.
2. Navigate to `android-slideshow/app/src/main/res`
3. Create a new directory named `values-<language_code>` where `<language_code>` is the code from [ISO 639.1][2]. e.g. `values-it` for Italian
4. Copy `values/strings.xml` into your new `values-<language_code>` directory.
5. Translate each of the resource strings into your language. (Please do not translate or alter the links or author name).
6. Commit your changes **with `#26 <language> translation` in the commit comment**, where `<language>` is your language.
7. Submit a pull request via Github.

Thanks a lot for this!

## Got an idea for a fancy feature?

Request your feature by [creating a ticket](https://github.com/ScreamingHawk/android-slideshow/issues/new).

Android Slideshow is intended to be simplistic and minimal, so features are subject to approval.

## Thank you!
To anyone who even looks at this page!!!

[1]: https://github.com/ScreamingHawk/android-slideshow/issues
[2]: http://www.loc.gov/standards/iso639-2/php/code_list.php
