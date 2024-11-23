package com.bulifier.core.ui.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.hideKeyboard() {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun <A : Any?, B : Any?> letAll(a: A?, b: B?, block: (A, B) -> Unit) {
    if (a == null || b == null) {
        return
    }

    block(a, b)
}

fun <A : Any?, B : Any?, C : Any?> letAll(a: A?, b: B?, c: C?, block: (A, B, C) -> Unit) {
    if (a == null || b == null || c == null) {
        return
    }

    block(a, b, c)
}

fun <A : Any?, B : Any?, C : Any?, D : Any?> letAll(
    a: A?,
    b: B?,
    c: C?,
    d: D?,
    block: (A, B, C, D) -> Unit
) {
    if (a == null || b == null || c == null || d == null) {
        return
    }

    block(a, b, c, d)
}