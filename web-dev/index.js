#!/usr/bin/env node
let {build} = require("esbuild");                                            
let fs = require('fs');                                                      
let Synci18n = require('sync-i18n');                                         
                                                                             
const sourceFolder = './web';                                                
                                                                             
Synci18n().generateTranslations();
                                                                             
var concatContents = '';
const jsFiles = fs.readdirSync(sourceFolder)
  .filter(f => f.endsWith('.js'));
console.log('concatenating files: ', jsFiles);
for (var file of jsFiles) {
  concatContents += fs.readFileSync('web/' + file, {encoding: 'utf8'});
}

build({
  stdin: {contents: concatContents, resolveDir: sourceFolder},
  outfile: "./target/plugin.js",
  minify: true,
  format: 'iife',
  bundle: true
}).catch(e => {
  console.error(e);
  process.exit(1);
});
