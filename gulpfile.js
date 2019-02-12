var gulp = require('gulp');
var Synci18n = require('sync-i18n');
var concat = require('gulp-concat');
var uglify = require('gulp-uglify');

var webLocation = 'web';
var targetLocation = "target";

gulp.task('i18n', function (done) {
  Synci18n().generateTranslations();
  done();
});

gulp.task('prepare-package', function() {
  return gulp.src(webLocation + '/*.js')
    .pipe(concat('plugin.js'))
    .pipe(uglify())
    .pipe(gulp.dest(targetLocation));
});


gulp.task('default', gulp.series('i18n', 'prepare-package'));
