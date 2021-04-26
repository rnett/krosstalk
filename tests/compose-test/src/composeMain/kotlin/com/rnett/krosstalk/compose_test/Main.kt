package com.rnett.krosstalk.compose_test

import com.rnett.krosstalk.KrosstalkPluginApi

sealed class MainState {
    object LoginScreen : MainState()
    data class TodoView(val auth: AuthCredentials) : MainState()
}

@OptIn(KrosstalkPluginApi::class)
fun main() {
    val m = TodoKrosstalk.methods
    println(m)
//    Window("Krosstalk + Compose test") {
//        DesktopMaterialTheme {
//            var mainState: MainState by remember { mutableStateOf(MainState.LoginScreen) }
//
//            Column(Modifier.fillMaxSize().background(Color.DarkGray)){
//                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceAround){
//                    Text("TODO App")
//                }
//                when(val state = mainState){
//                    MainState.LoginScreen -> LoginScreen {
//                        mainState = MainState.TodoView(it)
//                    }
//                    is MainState.TodoView -> TodoView(state.auth){ mainState = MainState.LoginScreen }
//                }
//            }
//        }
//    }
}