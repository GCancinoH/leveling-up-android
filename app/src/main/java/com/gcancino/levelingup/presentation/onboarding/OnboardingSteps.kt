package com.gcancino.levelingup.presentation.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.gcancino.levelingup.R
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.bodyComposition.UnitSystem
import com.gcancino.levelingup.domain.models.bodyComposition.cmToInches
import com.gcancino.levelingup.domain.models.bodyComposition.inchesToCm
import com.gcancino.levelingup.domain.models.bodyComposition.kgToLbs
import com.gcancino.levelingup.domain.models.bodyComposition.lbsToKg
import com.gcancino.levelingup.domain.models.identity.IdentityStandard
import com.gcancino.levelingup.domain.models.identity.Role
import com.gcancino.levelingup.domain.models.identity.StandardType
import com.gcancino.levelingup.domain.models.onBoarding.OnboardingData
import com.gcancino.levelingup.domain.models.player.Genders
import com.gcancino.levelingup.domain.models.player.Improvement
import com.gcancino.levelingup.ui.components.bodyData.BodyDataField
import com.gcancino.levelingup.ui.components.bodyData.UnitToggle
import com.gcancino.levelingup.ui.theme.SystemColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.system.exitProcess
import androidx.core.graphics.toColorInt
import com.gcancino.levelingup.domain.logic.DateUtils

/**
 * Step 0: Welcome
 * Reuses your existing WelcomeStep dialog pattern but adapted for onboarding
 */
@ExperimentalMaterial3Api
@Composable
fun OnboardingWelcomeStep(onNext: () -> Unit) {
    var showWelcomeDialog  by remember { mutableStateOf(true) }
    var showCounterDialog  by remember { mutableStateOf(false) }
    val context            = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemColors.BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (showWelcomeDialog) {
            WelcomeDialog(
                ctx       = context,
                onDismiss = {
                    showWelcomeDialog = false
                    showCounterDialog = true
                }
            )
        }
        if (showCounterDialog) {
            CounterDialog(
                ctx       = context,
                onAccept  = {
                    showCounterDialog = false
                    onNext()
                },
                onDecline = { exitProcess(0) }
            )
        }
    }
}

/**
 * Step 1: Personal Info
 */
@ExperimentalMaterial3Api
@Composable
fun OnboardingPersonalInfoStep(
    initialName: String,
    initialBirthDate: Date?,
    initialAge: Int?,
    initialGender: Genders?,
    onNext: (name: String, birthDate: Date?, gender: Genders?) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    var name by remember { mutableStateOf(initialName) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var gender by remember { mutableStateOf(initialGender) }

    var showDatePicker by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && birthDate != null && gender != null

    val age by remember {
        derivedStateOf {
            birthDate?.let { DateUtils.calculateAge(it) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        birthDate = Date(it)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OnboardingStepLayout(
        title    = "Personal Info",
        subtitle = "Tell us about yourself so we can personalize your experience.",
        onBack   = onBack,
        onNext   = { onNext(name, birthDate, gender) },
        isValid  = isValid,
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value         = name,
            onValueChange = { name = it },
            label         = { Text("Display Name") },
            leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = birthDate?.let {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
            } ?: "",
            onValueChange = { },
            label = { Text("Birthday") },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            readOnly = true,
            enabled = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = age?.toString() ?: "",
            onValueChange = { },
            label = { Text("Age") },
            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Gender selector
        Text(
            "Gender",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Genders.entries.forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick  = { gender = g },
                    label    = { Text(g.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

// ─── Step 2: Physical Attributes + Body Composition (merged) ──────────────────────
@Composable
fun OnboardingPhysicalCompositionStep(
    initialData: OnboardingData,
    onNext: (height: Double, weight: Double, bmi: Double, bodyFat: Double,
             muscleMass: Double, visceralFat: Double, bodyAge: Int,
             unit: UnitSystem
    ) -> Unit,
    onBack: () -> Unit
) {
    var unitSystem by remember { mutableStateOf(initialData.unitSystem) }
    var height by remember { mutableStateOf(if (initialData.height > 0) initialData.height.fmt() else "") }
    var weight by remember { mutableStateOf(if (initialData.weight > 0) initialData.weight.fmt() else "") }
    var bmi by remember { mutableStateOf(if (initialData.bmi > 0) initialData.bmi.fmt() else "") }
    var bodyFat by remember { mutableStateOf(if (initialData.bodyFatPercentage > 0) initialData.bodyFatPercentage.fmt() else "") }
    var muscleMass by remember { mutableStateOf(if (initialData.muscleMassPercentage > 0) initialData.muscleMassPercentage.fmt() else "") }
    var visceralFat by remember { mutableStateOf(if (initialData.visceralFat > 0) initialData.visceralFat.fmt() else "") }
    var bodyAge by remember { mutableStateOf(if (initialData.bodyAge > 0) initialData.bodyAge.toString() else "") }

    // Auto-calculate BMI from height + weight
    LaunchedEffect(height, weight) {
        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        if (h != null && w != null && h > 0) {
            val heightM = if (unitSystem == UnitSystem.METRIC) h / 100.0 else h * 0.0254
            val weightKg = if (unitSystem == UnitSystem.METRIC) w else w * 0.453592
            bmi = (weightKg / (heightM * heightM)).fmt()
        }
    }

    val heightUnit  = if (unitSystem == UnitSystem.METRIC) "cm" else "in"
    val weightUnit  = if (unitSystem == UnitSystem.METRIC) "kg" else "lbs"

    val isValid = listOf(height, weight, bmi, bodyFat, muscleMass, visceralFat, bodyAge)
        .all { it.isNotBlank() && it.toDoubleOrNull() != null }

    OnboardingStepLayout(
        title    = "Physical & Composition",
        subtitle = "These are your baseline measurements for tracking progress.",
        onBack   = onBack,
        onNext   = {
            onNext(
                height.toDouble(), weight.toDouble(), bmi.toDouble(),
                bodyFat.toDouble(), muscleMass.toDouble(),
                visceralFat.toDouble(), bodyAge.toInt(),
                unitSystem
            )
        },
        isValid = isValid,
        onDismiss = { },
        headerExtra = {
            UnitToggle(unitSystem = unitSystem, onToggle = {
                val next = if (unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
                if (unitSystem == UnitSystem.METRIC) {
                    height = height.toDoubleOrNull()?.cmToInches()?.fmt() ?: height
                    weight = weight.toDoubleOrNull()?.kgToLbs()?.fmt() ?: weight
                } else {
                    height = height.toDoubleOrNull()?.inchesToCm()?.fmt() ?: height
                    weight = weight.toDoubleOrNull()?.lbsToKg()?.fmt() ?: weight
                }
                unitSystem = next
            })
        }
    ) {
        BodyDataField("Height",height,{ height = it }, heightUnit)
        BodyDataField("Weight",weight,{ weight = it }, weightUnit)
        BodyDataField("BMI",bmi,{ }, "", readOnly = true)
        BodyDataField("Body Fat",bodyFat,{ bodyFat = it }, "%")
        BodyDataField("Muscle Mass",muscleMass,{ muscleMass = it }, "%")
        BodyDataField("Visceral Fat", visceralFat,{ visceralFat = it },"")
        BodyDataField("Body Age",bodyAge,{ bodyAge = it }, "yrs")
    }
}

// ─── Step 3: Body Measurements ────────────────────────────────────────────────────

@Composable
fun OnboardingBodyMeasurementsStep(
    initialData: OnboardingData,
    onNext: (OnboardingData) -> Unit,
    onBack: () -> Unit
) {
    var unitSystem        by remember { mutableStateOf(initialData.unitSystem) }
    var neck              by remember { mutableStateOf(if (initialData.neck > 0) initialData.neck.fmt() else "") }
    var shoulders         by remember { mutableStateOf(if (initialData.shoulders > 0) initialData.shoulders.fmt() else "") }
    var chest             by remember { mutableStateOf(if (initialData.chest > 0) initialData.chest.fmt() else "") }
    var waist             by remember { mutableStateOf(if (initialData.waist > 0) initialData.waist.fmt() else "") }
    var umbilical         by remember { mutableStateOf(if (initialData.umbilical > 0) initialData.umbilical.fmt() else "") }
    var hip               by remember { mutableStateOf(if (initialData.hip > 0) initialData.hip.fmt() else "") }
    var bicepLeftRelaxed  by remember { mutableStateOf(if (initialData.bicepLeftRelaxed > 0) initialData.bicepLeftRelaxed.fmt() else "") }
    var bicepLeftFlexed   by remember { mutableStateOf(if (initialData.bicepLeftFlexed > 0) initialData.bicepLeftFlexed.fmt() else "") }
    var bicepRightRelaxed by remember { mutableStateOf(if (initialData.bicepRightRelaxed > 0) initialData.bicepRightRelaxed.fmt() else "") }
    var bicepRightFlexed  by remember { mutableStateOf(if (initialData.bicepRightFlexed > 0) initialData.bicepRightFlexed.fmt() else "") }
    var forearmLeft       by remember { mutableStateOf(if (initialData.forearmLeft > 0) initialData.forearmLeft.fmt() else "") }
    var forearmRight      by remember { mutableStateOf(if (initialData.forearmRight > 0) initialData.forearmRight.fmt() else "") }
    var thighLeft         by remember { mutableStateOf(if (initialData.thighLeft > 0) initialData.thighLeft.fmt() else "") }
    var thighRight        by remember { mutableStateOf(if (initialData.thighRight > 0) initialData.thighRight.fmt() else "") }
    var calfLeft          by remember { mutableStateOf(if (initialData.calfLeft > 0) initialData.calfLeft.fmt() else "") }
    var calfRight         by remember { mutableStateOf(if (initialData.calfRight > 0) initialData.calfRight.fmt() else "") }

    val unit = if (unitSystem == UnitSystem.METRIC) "cm" else "in"

    val allFields = listOf(
        neck, shoulders, chest, waist, umbilical, hip,
        bicepLeftRelaxed, bicepLeftFlexed, bicepRightRelaxed, bicepRightFlexed,
        forearmLeft, forearmRight, thighLeft, thighRight, calfLeft, calfRight
    )
    val isValid = allFields.all { it.isNotBlank() && it.toDoubleOrNull() != null }

    fun String.convert(from: UnitSystem): String {
        val v = toDoubleOrNull() ?: return this
        return if (from == UnitSystem.METRIC) v.cmToInches().fmt() else v.inchesToCm().fmt()
    }

    OnboardingStepLayout(
        title    = "Body Measurements",
        subtitle = "These baseline measurements track your physical progress over time.",
        onBack   = onBack,
        onNext   = {
            onNext(initialData.copy(
                unitSystem        = unitSystem,
                neck              = neck.toDouble(),
                shoulders         = shoulders.toDouble(),
                chest             = chest.toDouble(),
                waist             = waist.toDouble(),
                umbilical         = umbilical.toDouble(),
                hip               = hip.toDouble(),
                bicepLeftRelaxed  = bicepLeftRelaxed.toDouble(),
                bicepLeftFlexed   = bicepLeftFlexed.toDouble(),
                bicepRightRelaxed = bicepRightRelaxed.toDouble(),
                bicepRightFlexed  = bicepRightFlexed.toDouble(),
                forearmLeft       = forearmLeft.toDouble(),
                forearmRight      = forearmRight.toDouble(),
                thighLeft         = thighLeft.toDouble(),
                thighRight        = thighRight.toDouble(),
                calfLeft          = calfLeft.toDouble(),
                calfRight         = calfRight.toDouble()
            ))
        },
        isValid = isValid,
        onDismiss = {},
        headerExtra = {
            UnitToggle(unitSystem = unitSystem, onToggle = {
                val current = unitSystem
                neck              = neck.convert(current)
                shoulders         = shoulders.convert(current)
                chest             = chest.convert(current)
                waist             = waist.convert(current)
                umbilical         = umbilical.convert(current)
                hip               = hip.convert(current)
                bicepLeftRelaxed  = bicepLeftRelaxed.convert(current)
                bicepLeftFlexed   = bicepLeftFlexed.convert(current)
                bicepRightRelaxed = bicepRightRelaxed.convert(current)
                bicepRightFlexed  = bicepRightFlexed.convert(current)
                forearmLeft       = forearmLeft.convert(current)
                forearmRight      = forearmRight.convert(current)
                thighLeft         = thighLeft.convert(current)
                thighRight        = thighRight.convert(current)
                calfLeft          = calfLeft.convert(current)
                calfRight         = calfRight.convert(current)
                unitSystem = if (current == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
            })
        }
    ) {
        MeasurementSection("Core") {
            BodyDataField("Neck",      neck,      { neck = it },      unit)
            BodyDataField("Shoulders", shoulders, { shoulders = it }, unit)
            BodyDataField("Chest",     chest,     { chest = it },     unit)
            BodyDataField("Waist",     waist,     { waist = it },     unit)
            BodyDataField("Umbilical", umbilical, { umbilical = it }, unit)
            BodyDataField("Hip",       hip,       { hip = it },       unit)
        }
        MeasurementSection("Arms") {
            BodyDataField("Bicep Left (Relaxed)",  bicepLeftRelaxed,  { bicepLeftRelaxed = it },  unit)
            BodyDataField("Bicep Left (Flexed)",   bicepLeftFlexed,   { bicepLeftFlexed = it },   unit)
            BodyDataField("Bicep Right (Relaxed)", bicepRightRelaxed, { bicepRightRelaxed = it }, unit)
            BodyDataField("Bicep Right (Flexed)",  bicepRightFlexed,  { bicepRightFlexed = it },  unit)
            BodyDataField("Forearm Left",          forearmLeft,       { forearmLeft = it },       unit)
            BodyDataField("Forearm Right",         forearmRight,      { forearmRight = it },      unit)
        }
        MeasurementSection("Legs") {
            BodyDataField("Thigh Left",  thighLeft,  { thighLeft = it },  unit)
            BodyDataField("Thigh Right", thighRight, { thighRight = it }, unit)
            BodyDataField("Calf Left",   calfLeft,   { calfLeft = it },   unit)
            BodyDataField("Calf Right",  calfRight,  { calfRight = it },  unit)
        }
    }
}

// ─── Step 4: Improvements ─────────────────────────────────────────────────────────

@Composable
fun OnboardingImprovementsStep(
    initialSelections: List<Improvement>,
    onNext: (List<Improvement>) -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf(initialSelections.toSet()) }

    OnboardingStepLayout(
        title    = "Your Goals",
        subtitle = "Select the areas you want to improve. Choose as many as you like.",
        onBack   = onBack,
        onNext   = { onNext(selected.toList()) },
        isValid  = selected.isNotEmpty(),
        onDismiss = { },
    ) {
        Improvement.entries.forEach { improvement ->
            val isSelected = improvement in selected
            FilterChip(
                selected = isSelected,
                onClick  = {
                    selected = if (isSelected) selected - improvement else selected + improvement
                },
                label    = {
                    Text(improvement.name.replace("_", " ")
                        .lowercase().replaceFirstChar { it.uppercase() })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

// ─── Step 5: Photos ───────────────────────────────────────────────────────────────

@Composable
fun OnboardingPhotosStep(
    selectedUris: List<Uri>,
    saveState: Resource<Unit>?,
    onAddPhoto: (Uri) -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> uris.forEach { onAddPhoto(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Text(
                "Progress Photos",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add photos to track your visual progress over time. This step is optional — you can add photos later.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Photos", color = MaterialTheme.colorScheme.primary)
            }

            if (selectedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedUris) { uri ->
                        Box {
                            AsyncImage(
                                model        = uri,
                                contentDescription = null,
                                modifier     = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick  = { onRemovePhoto(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove",
                                    tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // ── Navigation buttons ────────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("Back", color = Color.Gray)
            }
            Button(
                onClick  = onFinish,
                enabled  = saveState !is Resource.Loading,
                colors   = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                if (saveState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Finish", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Onboarding IdentityStep
@Composable
fun OnboardingIdentityStep(
    initialStatement: String,
    initialRoles: List<Role>,
    initialStandards: List<IdentityStandard>,
    onNext: (statement: String, roles: List<Role>, standards: List<IdentityStandard>) -> Unit,
    onBack: () -> Unit
) {
    var statement    by remember { mutableStateOf(initialStatement) }
    var roles        by remember { mutableStateOf(initialRoles) }
    var standards    by remember { mutableStateOf(initialStandards) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showRoleSheet by remember { mutableStateOf(false) }
    var addStandardForRole by remember { mutableStateOf<Role?>(null) }

    val hasTraining = standards.any { it.type == StandardType.TRAINING }
    val isValid     = statement.isNotBlank() && roles.isNotEmpty() && standards.isNotEmpty()

    OnboardingStepLayout(
        title    = stringResource(R.string.identity_title),
        subtitle = stringResource(R.string.identity_subtitle),
        onBack   = onBack,
        onNext   = { onNext(statement, roles, standards) },
        onDismiss = { onNext(statement, roles, standards) },
        isValid  = isValid
    ) {
        // ── Identity statement ────────────────────────────────────────────────────
        Text(
            stringResource(R.string.identity_prefix) + "...",
            style      = MaterialTheme.typography.labelLarge,
            color      = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value         = statement,
            onValueChange = { statement = it },
            placeholder   = { Text(stringResource(R.string.identity_placeholder), color = Color.DarkGray) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            prefix        = { Text(stringResource(R.string.identity_prefix), color = Color.Gray) },
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor     = Color.White,
                unfocusedTextColor   = Color.White,
                cursorColor          = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.identity_daily_intro),
            style  = MaterialTheme.typography.bodySmall,
            color  = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Roles section ─────────────────────────────────────────────────────────
        Text(
            stringResource(R.string.identity_my_roles),
            style         = MaterialTheme.typography.labelLarge,
            color         = MaterialTheme.colorScheme.primary,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (roles.isEmpty()) {
            Text(
                stringResource(R.string.identity_no_roles_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        roles.forEach { role ->
            RoleDeclaredRow(
                role       = role,
                onRemove   = {
                    roles = roles.filter { it.id != role.id }
                    standards = standards.filter { it.roleId != role.id }
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (roles.size < 5) {
            OutlinedButton(
                onClick  = { showRoleSheet = true },
                modifier = Modifier.fillMaxWidth(),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.identity_add_role), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Standards grouped by role ─────────────────────────────────────────────
        if (roles.isNotEmpty()) {
            Text(
                stringResource(R.string.identity_daily_standards_header),
                style         = MaterialTheme.typography.labelLarge,
                color         = MaterialTheme.colorScheme.primary,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Training suggestion (attached to first role if exists)
            if (!hasTraining && roles.isNotEmpty()) {
                val trainLabel = stringResource(R.string.identity_train_label)
                StandardSuggestionChip(
                    icon  = Icons.Default.FitnessCenter,
                    label = trainLabel,
                    color = Color(0xFF7986CB),
                    hint  = stringResource(R.string.identity_train_hint),
                    onClick = {
                        standards = standards + IdentityStandard(
                            id = UUID.randomUUID().toString(),
                            roleId = roles.firstOrNull()?.id ?: return@StandardSuggestionChip,
                            uID = "",
                            title = trainLabel,
                            type = StandardType.TRAINING,
                            xpReward = 25
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Group standards by role
            roles.forEach { role ->
                val roleStandards = standards.filter { it.roleId == role.id }

                // Role header
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        role.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        role.name,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (roleStandards.size < 5) {
                        IconButton(
                            onClick  = { addStandardForRole = role },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Standards for this role
                roleStandards.forEach { standard ->
                    StandardDeclaredRow(
                        standard = standard,
                        onRemove = { standards = standards.filter { it.id != standard.id } }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (roleStandards.isEmpty()) {
                    Text(
                        stringResource(R.string.identity_add_standard),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color.Gray,
                        modifier = Modifier
                            .padding(start = 28.dp, bottom = 8.dp)
                            .clickable { addStandardForRole = role }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info note
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1C1C2E)
        ) {
            Text(
                stringResource(R.string.identity_info_note),
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.Gray,
                modifier = Modifier.padding(12.dp),
                lineHeight = 18.sp
            )
        }
    }

    // ── Sheet to add standard ─────────────────────────────────────────────────────
    if (showAddSheet || addStandardForRole != null) {
        val targetRole = addStandardForRole
        AddStandardSheet(
            roles         = roles,
            preselectedRole = targetRole,
            onAdd         = { title, type, xp, roleId ->
                standards = standards + IdentityStandard(
                    id       = UUID.randomUUID().toString(),
                    roleId   = roleId,
                    uID      = "",
                    title    = title,
                    type     = type,
                    xpReward = xp
                )
                showAddSheet = false
                addStandardForRole = null
            },
            onDismiss = {
                showAddSheet = false
                addStandardForRole = null
            }
        )
    }

    // ── Sheet to add role ─────────────────────────────────────────────────────────
    if (showRoleSheet) {
        AddRoleSheet(
            onAdd     = { name, icon, color ->
                roles = roles + Role(
                    id    = UUID.randomUUID().toString(),
                    name  = name,
                    icon  = icon,
                    color = color
                )
                showRoleSheet = false
            },
            onDismiss = { showRoleSheet = false }
        )
    }
}

// ─── Auxiliary components ─────────────────────────────────────────────────────────

@Composable
private fun StandardSuggestionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    hint: String,
    onClick: () -> Unit
) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = color.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White, fontWeight = FontWeight.Medium)
                Text(hint, style = MaterialTheme.typography.labelSmall, color = color)
            }
            Icon(Icons.Default.Add, null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RoleDeclaredRow(
    role: Role,
    onRemove: () -> Unit
) {
    val roleColor = try { Color("#${role.color}".toColorInt()) } catch (_: Exception) { Color(0xFF7986CB) }
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .background(roleColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(role.icon, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(role.name, style = MaterialTheme.typography.bodyMedium,
            color = Color.White, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun StandardDeclaredRow(
    standard: IdentityStandard,
    onRemove: () -> Unit
) {
    val iconPainter = when (standard.type) {
        StandardType.TRAINING  -> rememberVectorPainter(Icons.Default.FitnessCenter)
        StandardType.NUTRITION -> rememberVectorPainter(Icons.Default.Restaurant)
        StandardType.SLEEP     -> rememberVectorPainter(Icons.Default.Bedtime)
        StandardType.MINDSET   -> painterResource(R.drawable.brain)
        StandardType.DEEP_WORK -> rememberVectorPainter(Icons.Default.Computer)
        StandardType.LEARNING  -> rememberVectorPainter(Icons.Default.AutoStories)
        StandardType.FINANCE   -> rememberVectorPainter(Icons.Default.AttachMoney)
        StandardType.CUSTOM    -> rememberVectorPainter(Icons.Default.Star)
    }

    val color = when (standard.type) {
        StandardType.TRAINING  -> Color(0xFF4FC3F7) // Electric Blue
        StandardType.NUTRITION -> Color(0xFF66BB6A) // Green
        StandardType.SLEEP     -> Color(0xFF5C6BC0) // Indigo
        StandardType.MINDSET   -> Color(0xFFBB86FC) // Purple
        StandardType.DEEP_WORK -> Color(0xFFFF7043) // Orange
        StandardType.LEARNING  -> Color(0xFF26A69A) // Teal
        StandardType.FINANCE   -> Color(0xFFFFCA28) // Amber
        StandardType.CUSTOM    -> Color(0xFF9E9E9E) // Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(standard.title, style = MaterialTheme.typography.bodySmall,
                color = Color.White, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.identity_xp_per_day, standard.xpReward),
                style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddStandardSheet(
    roles: List<Role>,
    preselectedRole: Role?,
    onAdd: (title: String, type: StandardType, xp: Int, roleId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StandardType.CUSTOM) }
    var selectedRoleId by remember { mutableStateOf(preselectedRole?.id ?: roles.firstOrNull()?.id ?: "") }
    val xpMap = mapOf(
        StandardType.TRAINING  to 25,
        StandardType.NUTRITION to 20,
        StandardType.SLEEP     to 15,
        StandardType.CUSTOM    to 10
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.identity_new_standard),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text(stringResource(R.string.identity_standard_question)) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor  = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor    = Color.White,
                    unfocusedTextColor  = Color.White,
                    focusedLabelColor   = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor         = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role selector
            Text(stringResource(R.string.identity_standard_role),
                style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                roles.forEach { role ->
                    FilterChip(
                        selected = selectedRoleId == role.id,
                        onClick  = { selectedRoleId = role.id },
                        label    = { Text("${role.icon} ${role.name}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type selector with FlowRow
            Text(stringResource(R.string.identity_standard_type),
                style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(4.dp)
            ) {
                StandardType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick  = { selectedType = type },
                        label    = {
                            Text(type.name.lowercase()
                                .replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.identity_standard_xp_hint, xpMap[selectedType] ?: 10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick  = {
                    if (title.isNotBlank() && selectedRoleId.isNotBlank()) {
                        onAdd(title.trim(), selectedType, xpMap[selectedType] ?: 10, selectedRoleId)
                    }
                },
                enabled  = title.isNotBlank() && selectedRoleId.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.identity_add_standard_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRoleSheet(
    onAdd: (name: String, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var roleName by remember { mutableStateOf("") }
    val icons = listOf("⚡", "🏋️", "📈", "📚", "🎯", "💻", "🎨", "🧘", "🍎", "💤")
    var selectedIcon by remember { mutableStateOf(icons.firstOrNull() ?: "⚡") }
    val colors = listOf("4FC3F7", "66BB6A", "BB86FC", "FF7043", "EF5350", "FFCA28", "26A69A", "F06292")
    var selectedColor by remember { mutableStateOf(colors.firstOrNull() ?: "4FC3F7") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.identity_new_role),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value         = roleName,
                onValueChange = { roleName = it },
                label         = { Text(stringResource(R.string.identity_role_name_label)) },
                placeholder   = { Text(stringResource(R.string.identity_role_name_placeholder), color = Color.DarkGray) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor     = Color.White,
                    unfocusedTextColor   = Color.White,
                    focusedLabelColor    = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor  = Color.Gray,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Icon picker
            Text("Icon", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                icons.forEach { icon ->
                    val isSelected = selectedIcon == icon
                    Surface(
                        shape    = CircleShape,
                        color    = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                        border   = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { selectedIcon = icon }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(icon, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color picker
            Text("Color", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                colors.forEach { hex ->
                    val c = try { Color("#$hex".toColorInt()) } catch (_: Exception) { Color(0xFF7986CB) }
                    val isSelected = selectedColor == hex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(c, CircleShape)
                            .then(
                                if (isSelected) Modifier.background(Color.Transparent, CircleShape)
                                else Modifier
                            )
                            .clickable { selectedColor = hex }
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick  = { if (roleName.isNotBlank()) onAdd(roleName.trim(), selectedIcon, selectedColor) },
                enabled  = roleName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.identity_add_role), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Shared layout for steps
@Composable
fun OnboardingStepLayout(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    isValid: Boolean,
    headerExtra: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // ── Header: title, subtitle, then toggle aligned to the right ─────────
            Text(
                text       = title,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.bodySmall,
                color    = Color.Gray,
                //modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(7.dp))
            if (headerExtra != null) {
                headerExtra()
            }
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SystemColors.BackgroundColor)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("Back", color = Color.Gray)
            }
            TextButton(onClick = onDismiss) {
                Text("Skip", color = Color.Gray)
            }
            Button(
                onClick  = onNext,
                enabled  = isValid,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color.White,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────────

@Composable
private fun MeasurementSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.DarkGray)
    Text(
        text          = title.uppercase(),
        style         = MaterialTheme.typography.labelLarge,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(bottom = 8.dp)
    )
    Column(content = content)
}

private fun Double.fmt(): String = String.format(Locale.US, "%.1f", this)