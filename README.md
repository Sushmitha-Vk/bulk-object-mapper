# Bulk Object Mapper Extension

## Description

## Configuration

Here are the default alfresco global properties settings

```
bulk.object.content.url.check.mode=STORE
s3.bucketLocation=eu-west-2
s3.bucketName=none
s3.awsKmsKeyId=none
bulk.object.default.batch_size=100
bulk.object.default.pool_size=2
```

The valid values for `bulk.object.content.url.check.mode` are:

* `STORE` -- normal checks against filesystem based content stores
* `S3` -- checks optimized for S3 buckets (with a fallback to the file system content store for non S3 content URLs)
* `FALSE` -- checks always evaluate to false (debugging only)
* `TRUE` -- checks always evaluate to true (debugging only)
* `DIRECTED` -- unused

## S3 Dependecies

If you use S3 you will need to update your runner pom.xml include the following dependencies

```

		<dependency>
		    <groupId>com.amazonaws</groupId>
		    <artifactId>aws-java-sdk-core</artifactId>
		    <version>1.11.88</version>
		</dependency>
		<dependency>
		    <groupId>com.amazonaws</groupId>
		    <artifactId>aws-java-sdk-kms</artifactId>
		    <version>1.11.88</version>
		</dependency>
		<dependency>
		    <groupId>com.amazonaws</groupId>
		    <artifactId>aws-java-sdk-s3</artifactId>
		    <version>1.11.88</version>
		</dependency>
```

## Bulk Object Mapper API

### REST Example

`http://localhost:8080/alfresco/s/bulkobj/mapobjects/{uuid}`

*Parameters*

* `checkParents` this will force it to look at any information about primary parents in the manifest (including the `path` parameter.  Otherwise it will assume that all objects are direct chidren of the nodeRef in the UUID.
* `autoCreate` this will autocreate any missing paths (implies that `checkParents` is true.

The body will contain the manifest

The current return value is always Status: Good

All nodes with "hanging content URLs" will be marked with the `bulkobj:missingContent` Aspect and have information about what content URL was expected.  (No `d:content` property will be created)

*Example URL*

`http://localhost:8080/alfresco/s/bulkobject/mapobjects/5b290d32-fa5b-4238-9c69-1deec103e072?autoCreate=y`


### Curl exampled used for testing

```
$ curl -uadmin:admin http://localhost:8080/alfresco/s/bulkobj/mapobjects/48fc94a7-633e-49b4-8827-a3e3f1db92bd?autoCreate=y -X POST -T - <<EOF
[{
		"type": "cm:content",
		"properties": {
			"cm:name": "x.txt",
			"cm:title": "X Factor"
		},
		"contentUrls": {
			"cm:content": "store://input/x.txt"
		}
	}, {
		"type": "cm:content",
		"path": "comment",
		"properties": {
			"cm:name": "comment.txt",
			"cm:title": "the comment"
		},
		"contentUrls": {
			"cm:content": "store://input/comment.txt"
		}
	},
	{
		"type": "cm:content",
		"path": "comment",
		"properties": {
			"cm:name": "comment2.txt",
			"cm:title": "the comment"
		},
		"contentUrls": {
			"cm:content": "store://input/comment2.txt"
		}
	}
]
EOF
```

### JS Example

`bulkobj.mapInPlaceObjectsFromString(ScriptNode parent, String jsonArray,boolean checkParents,boolean autoCreate);`

```
var manifest=[{
        "type": "cm:content",
        "properties": {
            "cm:name": "x.txt",
            "cm:title": "X Factor"
        },
        "contentUrls": {
            "cm:content": "store://input/x.txt"
        }
    }, {
        "type": "cm:content",
        "path": "comment",
        "properties": {
            "cm:name": "comment.txt",
            "cm:title": "the comment"
        },
        "contentUrls": {
            "cm:content": "store://input/comment.txt"
        }
    },
    {
        "type": "cm:content",
        "path": "comment",
        "properties": {
            "cm:name": "comment2.txt",
            "cm:title": "the comment"
        },
        "contentUrls": {
            "cm:content": "store://input/comment2.txt"
        }
    }
]


logger.log(JSON.stringify(manifest));
bulkobj.mapInPlaceObjectsFromString(document,JSON.stringify(manifest),true,true);
```


## Manifest Format

```
[
    {
        "type": "cm:content",
        "properties": {
            "cm:name": "x.txt",
            "cm:title": "X Factor"
        },
        "contentUrls": {
            "cm:content": "store://input/x.txt"
        }
    },
    {
        "type": "cm:content",
        "path": "comment",
        "properties": {
            "cm:name": "comment.txt",
            "cm:title": "the comment"
        },
        "contentUrls": {
            "cm:content": "store://input/comment.txt"
        }
    },
    {
        "type": "cm:content",
        "path": "comment",
        "properties": {
            "cm:name": "comment2.txt",
            "cm:title": "the comment"
        },
        "contentUrls": {
            "cm:content": "store://input/comment2.txt"
        }
    }
]
```
### Manifest with a version history
```

[
    {
        "type": "cm:content",
        "properties": {
            "cm:name": "x.txt",
            "cm:title": "X Factor"
        },
        "contentUrls": {
            "cm:content": "store://input/x.txt"
        }
    },
    {
        "type": "cm:content",
        "path": "comment",
        "versionHistory":
        [
            {
                "versionLabel": "1.0",
                "versionComment": "This is v1",
                "properties": {
                    "cm:name": "comment.txt",
                    "cm:title": "the comment"
                },
                "contentUrls": {
                    "cm:content": "store://input/comment.txt"
                }        
            },
            {
                "versionLabel": "1.1",
                "versionComment": "This is v1.1",
                "properties": {
                    "cm:title": "the new comment"
                },
                "contentUrls": {
                    "cm:content": "store://input/comment.txt"
                }        
            },
            {
                "versionLabel": "3.0",
                "versionComment": "This is v3",
                "properties": {
                    "cm:title": "the new comment"
                },
                "contentUrls": {
                    "cm:content": "store://input/comment3.txt"
                }        
            }

        ]
    },
    {
        "type": "cm:content",
        "path": "comment",
        "properties": {
            "cm:name": "comment2.txt",
            "cm:title": "the comment"
        },
        "contentUrls": {
            "cm:content": "store://input/comment2.txt"
        }
    }
]
```
## Bulk Update Commands API

### Building a Bulk Update Command Class

```
public interface BulkUpdateCommand {
	String commandName();
	Map<NodeRef,JSONObject> parseJson(JSONObject data,JSONArray list, JSONObject ctx);
	void preExec(JSONObject ctx,Map<NodeRef,JSONObject> map);	
	void preTxn(JSONObject ctx);	
	void workUnit(NodeRef nodeRef, JSONObject params, JSONObject ctx);
	void postTxn(JSONObject ctx);
	void postExec(JSONObject ctx);
}

```


### Structure of a Command Object

```
{ 
  command: "ATTACH_CONTENT", 
  data: { projectId: "C0355" } 
  list: []
}
```

### REST Examples

#### Submit a Command for processing

```
$ curl -uadmin:admin http://localhost:8080/alfresco/s//bulkobj/cmd?bgRun=Y -X POST -T cmd.json  # Run in background
$ curl -uadmin:admin http://localhost:8080/alfresco/s//bulkobj/cmd -X POST -T cmd.json  # Run in foreground
$ curl -uadmin:admin http://localhost:8080/alfresco/s//bulkobject/stats # Get an array of all of the stat GUIDs
$ curl -uadmin:admin http://localhost:8080/alfresco/s//bulkobject/stat/{uuid} # Get the status object
```

### JS Examples

```
var cmdObj = { command: "ATTACH_CONTENT", data: { projectId: "C0355" } } ;
logger.log(bulkObjectMapper.bulkExecuteCommandBG(JSON.stringify(cmdObj)));
```

```
	var ids = JSON.parse(bulkObjectMapper.getBackgroundCacheItemIds());
	for (i in ids) {
		logger.log(bulkObjectMapper.getBackgroundCacheItemAsJson(ids[i]));
	}

```