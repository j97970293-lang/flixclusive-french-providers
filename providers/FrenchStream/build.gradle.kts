import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.core.stubs.provider)
    implementation(libs.jsoup)
    compileOnly(libs.okhttp)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(platform(libs.compose.bom))
    compileOnly(libs.compose.material3)
    compileOnly(libs.compose.foundation)
    compileOnly(libs.compose.ui)
    compileOnly(libs.compose.runtime)
}

android {
    namespace = "com.flixclusive.provider.frenchstream"
}

flxProvider {
    id = "flx-frenchstream-7c3b2a91"
    providerName = "FrenchStream"
    description = "Catalogue français de films et séries avec recherche, métadonnées et résolution de lecteurs publics."
    versionMajor = 1
    versionMinor = 0
    versionPatch = 0
    versionBuild = 0
    language = Language("fr")
    providerType = ProviderType.All
    status = ProviderStatus.Beta
    requiresResources = true
}
