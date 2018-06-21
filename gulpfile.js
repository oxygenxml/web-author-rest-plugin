var gulp = require('gulp');
var Synci18n = require('sync-i18n');
var concat = require('gulp-concat');
var uglify = require('gulp-uglify');

var webLocation = 'web';
var targetLocation = "target";

gulp.task('prepare-package', ['i18n'], function() {
  return gulp.src(webLocation + '/*.js')
    .pipe(concat('plugin.js'))
    .pipe(uglify())
    .pipe(gulp.dest(targetLocation));
});

gulp.task('i18n', function () {
  Synci18n().generateTranslations();
});

gulp.task('default', ['prepare-package']);