nodes = search.query({ language: "fts-alfresco", query: "ASPECT:'mdrbulkobject:missingContent'" });
for (i in nodes) {
	bulkobj.tryToAttachContent(nodes[i]);
}
