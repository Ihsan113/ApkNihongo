package id.vixnihon.nativeapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact, reusable native field with an external label and consistent focus state.
 *  No default floating label: the field does not jump/resize when users start typing.
 */
@Composable
internal fun NihongoFormField(
    value: String,
    onValueChange: (String) -> Unit,
    title: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = NihongoPalette.white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(color = NihongoPalette.white, fontSize = 14.sp, lineHeight = 20.sp),
            placeholder = { Text(placeholder, color = NihongoPalette.dim, fontSize = 13.sp) },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = NihongoPalette.white,
                unfocusedTextColor = NihongoPalette.white,
                focusedBorderColor = NihongoPalette.redSoft,
                unfocusedBorderColor = NihongoPalette.borderStrong,
                focusedContainerColor = NihongoPalette.surface2,
                unfocusedContainerColor = NihongoPalette.surface2,
                cursorColor = NihongoPalette.redSoft,
                focusedPlaceholderColor = NihongoPalette.dim,
                unfocusedPlaceholderColor = NihongoPalette.dim,
            ),
        )
        if (!supportingText.isNullOrBlank()) {
            Text(supportingText, color = NihongoPalette.dim, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
