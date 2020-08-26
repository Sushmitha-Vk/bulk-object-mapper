var bgRun = false;
var autoCreate = false;
var checkParents = false;
var uuid = url.templateArgs["uuid"];
var path = args["path"];
var doc = null
if (uuid != null) {
	doc = utils.getNodeFromString("workspace://SpacesStore/" + uuid);
}
if ((doc == null || !doc.exists()) && path != null) {
	doc = companyhome.childByNamePath(path);
}
if ((doc == null || !doc.exists())) {
	doc = bulkobj.resolveImportRoot(args);
}
var objdata = { status: "good" };
if (doc == null || !doc.exists()) {
	objdata = { status: "failed", reason: "Could Not Resolve Parent Object" };
	status.code = 500;
	status.message = objdata.reason;
} else {
	if (args["autoCreate"] == "Y" || args["autoCreate"] == "y" || args["autoCreate"] == "1" ||
		args["autoCreate"] == "TRUE" || args["autoCreate"] == "true") {
		autoCreate = true;
		checkParents = true;
	}
	if (args["checkParents"] == "Y" || args["checkParents"] == "y" || args["checkParents"] == "1" ||
		args["checkParents"] == "TRUE" || args["checkParents"] == "true") {
		checkParents = true;
	}
	if (args["bgRun"] == "Y" || args["bgRun"] == "y" || args["bgRun"] == "1" ||
		args["bgRun"] == "TRUE" || args["bgRun"] == "true") {
		bgRun = true;
	}

	if (bgRun) {
		objdata.id = "" + bulkobj.mapInPlaceObjectsFromStringBG(doc, requestbody.content, checkParents, autoCreate);
	} else {
		bulkobj.mapInPlaceObjectsFromString(doc, requestbody.content, checkParents, autoCreate);
	}

}
model.obj = JSON.stringify(objdata);
