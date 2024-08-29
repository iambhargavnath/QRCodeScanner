package com.iambhargavnath.qrcodescanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _qrCodeText = MutableLiveData<String>()
    val qrCodeText: LiveData<String> get() = _qrCodeText

    fun updateQRCodeText(text: String) {
        _qrCodeText.value = text
    }
}