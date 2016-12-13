window.onload = function() {
  document.querySelector('#load').addEventListener('click', load);
};

function load() {
  var container = document.querySelector('#webapp');
  container.innerHTML = '';
  var options = getLoadOptions();

  options['url'] = 'http://vm17.sync.ro:8081/oxygen-webapp/static/oxygen.html',
  options['documentUrl'] = 'webdav-http://localhost:8081/oxygen-webapp/plugins-dispatcher/webdav-server/dita/flowers/topics/flowers/gardenia.dita'

  window.editor = new WebAuthor(container, options);

  console.log('Loading with options: ', options);

  document.querySelector('#read-only').addEventListener('click', function() {
    window.readonly = window.readonly ? false : true;
    // set READ_ONLY status on the editor.
    window.editor.setReadOnly(window.readonly, 'You do not have editing rights on this document.');
  });
}

function getLoadOptions() {
  var options = {};
  // forceTrackChanges option.
  var forceTrackChanges = document.querySelector('#forceTrackChanges').checked;
  if(forceTrackChanges) {
    options['forceTrackChanges'] = true;
  }
  return options;
}
