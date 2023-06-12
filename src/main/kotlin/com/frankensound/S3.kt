package com.frankensound

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import aws.smithy.kotlin.runtime.content.toByteArray

val client = S3Client { region = "eu-north-1" }

suspend fun getObject(bucketName: String, keyName: String, rangeValue: String?): ResponseS3? {
    val request = GetObjectRequest {
        key = keyName
        bucket = bucketName
        range = rangeValue
    }

    return client.getObject(request) { resp ->
        if (resp.body == null) {
            null
        } else {
            ResponseS3(resp.body!!.toByteArray(), resp.contentRange, resp.acceptRanges, resp.body!!.contentLength)
        }
    }
}

suspend fun listBucketObs(bucketName: String): ArrayList<String> {
    val list = ArrayList<String>()
    val request = ListObjectsRequest {
        bucket = bucketName
    }
    val response = client.listObjects(request)
    response.contents?.forEach { myObject ->
        list.add(myObject.key.toString())
    }
    return list
}

data class ResponseS3(
    val data: ByteArray,
    val contentRange: String?,
    val acceptRanges: String?,
    val contentLength: Long?,
)
