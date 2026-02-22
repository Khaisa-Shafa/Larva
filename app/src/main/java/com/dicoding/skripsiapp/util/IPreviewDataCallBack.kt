package com.dicoding.skripsiapp.util

interface IPreviewDataCallBack {
    fun onPreviewDataReceived(data: ByteArray, width: Int, height: Int, format: Int)
}