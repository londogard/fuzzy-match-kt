[![](https://jitpack.io/v/com.londogard/fuzzy-match-kt.svg)](https://jitpack.io/#com.londogard/fuzzy-match-kt)<a href='https://ko-fi.com/O5O819SEH' target='_blank'><img height='22' style='border:0px;height:22px;' src='https://az743702.vo.msecnd.net/cdn/kofi2.png?v=2' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

# fuzzy-match-kt
Fuzzy Matching in Kotlin. Great search. Blazing fast. Plain awesome.

## Usage
TODO()

## Installation
### Using jitpack
Add the following to your `build.gradle`. `$version` should be equal to the version supplied by tag above.
``` 
   repositories {
        maven { url "https://jitpack.io" }
   }
   dependencies {
         implementation 'com.github.jitpack:com.londogard:fuzzy-match-kt:$version'
   }
```
### Using Github Packages
Add the following to your `build.gradle`. `$version` should be equal to the version supplied by tag above.  
The part with logging into github repository is how I understand that you need to login. If you know a better way please ping me in an issue.
```
repositories {
   maven {
     url = uri("https://maven.pkg.github.com/londogard/fuzzy-match-kt")
     credentials {
         username = project.findProperty("gpr.user") ?: System.getenv("GH_USERNAME")
         password = project.findProperty("gpr.key") ?: System.getenv("GH_TOKEN")
     }
}
}
dependencies {
   implementation "com.londogard:fuzzy-match-kt:$version"
}
```
