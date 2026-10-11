const fs = require('fs');
const path = require('path');
const ts = require('../frontend-vue/node_modules/typescript');
const configPath = path.resolve('frontend-vue/tsconfig.app.json');
const config = ts.readConfigFile(configPath, ts.sys.readFile);
const parsed = ts.parseJsonConfigFileContent(config.config, ts.sys, path.dirname(configPath));
const program = ts.createProgram(parsed.fileNames, parsed.options);
const checker = program.getTypeChecker();
const flags = ts.TypeFormatFlags.NoTruncation | ts.TypeFormatFlags.UseAliasDefinedOutsideCurrentScope;
const typ = n => n.type ? n.type.getText() : checker.typeToString(checker.getTypeAtLocation(n), n, flags);
const clean = s => s.replace(/import\("[^"]+"\)\./g, '').replace(/\s+/g, ' ');
function op(n, visibility = 'public') {
  const signature = checker.getSignatureFromDeclaration(n);
  return {name:n.name.getText(), visibility, type:clean(n.type ? n.type.getText() : checker.typeToString(checker.getReturnTypeOfSignature(signature), n, flags)),
    params:n.parameters.map(p=>({name:p.name.getText(),type:clean(typ(p)),optional:!!p.questionToken,initial:p.initializer?.getText()})),body:n.body?.getText() || ''};
}
const data=[];
function add(file,name,kind,fields=[],ops=[]) { const item={file,name,kind,fields,ops}; data.push(item); return item; }
for (const relative of ['services/examApi.ts','services/violationApi.ts','services/examRealtime.ts','services/api.ts','types/domain.ts','composables/useExamManagement.ts']) {
 const file='frontend-vue/src/'+relative, sf=program.getSourceFile(path.resolve(file));
 const main=relative.split('/').pop().replace('.ts','');
 const module=main==='domain'?null:add(file,main,main==='useExamManagement'?'composable':'module');
 for(const n of sf.statements) {
  if(ts.isInterfaceDeclaration(n)) {
   if(main==='domain'&&!['SchoolClass','UserResponse','ExamSession','Question','Answer','ExamAttempt','ExamStudentAttempt','ExamResult','ExamDetails','RealtimeEvent'].includes(n.name.text))continue;
   add(file,n.name.text,'interface',n.members.filter(ts.isPropertySignature).map(p=>({name:p.name.getText(),type:clean(typ(p)),optional:!!p.questionToken,visibility:'public'})));
  } else if(ts.isTypeAliasDeclaration(n)) {
   if(main==='domain'&&!['Role','ExamStatus'].includes(n.name.text))continue;
   const item=add(file,n.name.text,'type'); item.definition=clean(n.type.getText());
  } else if(ts.isFunctionDeclaration(n)) {
   if(main!=='useExamManagement') { if(n.name.text!=='createExam')module.ops.push(op(n)); continue; }
   const returned=n.body.statements.find(ts.isReturnStatement).expression.properties.map(p=>p.name.getText());
   const options=data.find(x=>x.name==='UseExamManagementOptions');
   module.fields.push(...options.fields.map(p=>({...p,visibility:'private'})));
   for(const s of n.body.statements) {
    if(ts.isFunctionDeclaration(s)) {
     if(!['createExam','handleExamFileChange','resetExamForm'].includes(s.name.text))module.ops.push(op(s,returned.includes(s.name.text)?'public':'private'));
    } else if(ts.isVariableStatement(s)) {
     for(const v of s.declarationList.declarations)if(ts.isIdentifier(v.name))module.fields.push({name:v.name.text,type:clean(typ(v)),visibility:returned.includes(v.name.text)?'public':'private'});
    }
   }
   module.definition='Factory: useExamManagement(options: UseExamManagementOptions). UML public = returned functions/state; private = closure implementation. Creation/import operations omitted from this use case.';
  } else if(module && ts.isVariableStatement(n)) {
   for(const v of n.declarationList.declarations)if(ts.isIdentifier(v.name))module.fields.push({name:v.name.text,type:clean(typ(v)),visibility:'public',initial:v.initializer?.getText()});
  }
 }
}
fs.writeFileSync('xml_UML_diagram/.impl_frontend.json',JSON.stringify(data,null,2));
console.log('Extracted '+data.length+' frontend classifiers with compiler-inferred types.');
