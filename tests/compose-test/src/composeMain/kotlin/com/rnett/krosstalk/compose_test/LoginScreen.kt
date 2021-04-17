package com.rnett.krosstalk.compose_test

//@Composable
//fun LoginScreen(afterLogin: (AuthCredentials) -> Unit) {
//    Box(Modifier.fillMaxWidth(0.4f), contentAlignment = Alignment.Center) {
//        Surface(
//            shape = RoundedCornerShape(10.dp),
//            color = Color.LightGray,
//            border = BorderStroke(2.dp, Color.Gray),
//            elevation = 10.dp
//        ) {
//            var username by remember { mutableStateOf("") }
//            var password by remember { mutableStateOf("") }
//
//            var isLogingIn by remember { mutableStateOf(false) }
//            var isError by remember { mutableStateOf(false) }
//
//            Column(Modifier.padding(20.dp)) {
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                    Text("Username:")
//                    TextField(
//                        username,
//                        {
//                            isError = false
//                            username = it
//                        },
//                        readOnly = isLogingIn,
//                        isError = isError,
//                        singleLine = true
//                    )
//                }
//
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                    Text("Password:")
//                    TextField(
//                        password,
//                        {
//                            isError = false
//                            password = it
//                        },
//                        readOnly = isLogingIn,
//                        isError = isError,
//                        singleLine = true,
//                        visualTransformation = PasswordVisualTransformation()
//                    )
//                }
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
//                    if (!isLogingIn) {
//                        Button({
//                            isLogingIn = true
//                        }) {
//                            Text("Login")
//                        }
//                    } else {
//                        LaunchedEffect(username, password, isLogingIn) {
//                            if (tryLogin(username, password)) {
//                                afterLogin(TodoKrosstalk.Auth(username, password))
//                            } else {
//                                username = ""
//                                password = ""
//                                isError = true
//                                isLogingIn = false
//                            }
//                        }
//                        CircularProgressIndicator()
//                    }
//                }
//            }
//        }
//    }
//}