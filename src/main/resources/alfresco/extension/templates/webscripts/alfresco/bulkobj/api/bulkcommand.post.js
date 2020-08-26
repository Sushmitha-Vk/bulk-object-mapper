var bgRun = false;
var objdata = { status: "good" };
if (args["bgRun"] == "Y" || args["bgRun"] == "y" || args["bgRun"] == "1" ||
	args["bgRun"] == "TRUE" || args["bgRun"] == "true") {
	bgRun = true;
}
if (bgRun) {
	objdata.id = "" + bulkobj.bulkExecuteCommandBG(requestbody.content);
} else {
	bulkobj.bulkExecuteCommand(requestbody.content);
}
model.obj = JSON.stringify(objdata);