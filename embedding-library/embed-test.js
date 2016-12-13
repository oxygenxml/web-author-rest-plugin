window.onload = function() {
  var container = document.querySelector('#webapp');
  var options = {
    forceTrackChanges: true,

    url: 'http://vm17.sync.ro:8081/oxygen-webapp/static/oxygen.html',
    documentUrl: 'webdav-http://localhost:8081/oxygen-webapp/plugins-dispatcher/webdav-server/dita/flowers/topics/flowers/gardenia.dita'
  };

  window.editor = new WebAuthor(container, options);


  document.querySelector('#read-only').addEventListener('click', function() {
    window.readonly = window.readonly ? false : true;
    // set READ_ONLY status on the editor.
    window.editor.setReadOnly(window.readonly, 'You do not have editing rights on this document.');
  });

};