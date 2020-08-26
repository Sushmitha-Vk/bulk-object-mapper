var typename = url.templateArgs["typename"];
model.obj = bulkobj.getTypeInformation(typename);
model.jobj = JSON.parse(bulkobj.getTypeInformation(typename));