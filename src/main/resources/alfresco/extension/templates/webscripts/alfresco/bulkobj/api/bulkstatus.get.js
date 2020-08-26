var uuid = url.templateArgs["uuid"];
var obj;
if (uuid == null) {
	obj = bulkobj.getAllBackgroundItems();
} else {
	obj = bulkobj.getBackgroundCacheItemAsJson(uuid);
}
model.obj = obj;