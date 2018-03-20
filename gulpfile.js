var gulp = require('gulp');
var syncI18N = require('sync-i18n');


gulp.task('default', function () {
  var sourceFile = './i18n/translation.xml';
  var destinationFolder = __dirname + '/web';

  var synci18n = new syncI18N({
    sourceFile,
    destinationFolder
  });

  synci18n.makeTranslationJsons();
  synci18n.makeMsgs();
});
