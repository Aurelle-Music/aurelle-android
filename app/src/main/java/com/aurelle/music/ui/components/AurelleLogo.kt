package com.aurelle.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aurelle.music.R

@Composable
fun AurelleLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.aurelle_logo),
        contentDescription = stringResource(R.string.logo_description),
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
    )
}
