package com.rnett.krosstalk.compose_test

//@Composable
//fun TodoView(auth: AuthCredentials, logout: () -> Unit){
//    key(auth) {
//        var todos: List<ToDo>? by remember { mutableStateOf(null) }
//
//        LaunchedEffect(auth){
//            todos = getTodos(auth)
//        }
//
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//
//            if (todos == null) {
//                CircularProgressIndicator()
//            } else {
//                todos!!.forEach {
//                    DisplayTodo(it)
//                }
//            }
//
//            var addingMessage: String? by remember { mutableStateOf(null) }
//            var isAdding: Boolean by remember{ mutableStateOf(false) }
//
//            if(addingMessage == null){
//                IconButton({ addingMessage = "" }){
//                    Icon(Icons.Default.Add, "Add Todo")
//                }
//            } else {
//                Row {
//                    IconButton({
//                        addingMessage = null
//                    }, enabled = !isAdding){
//                        Icon(Icons.Default.Close, "Cancel")
//                    }
//                    Spacer(Modifier.width(2.dp))
//                    TextField(addingMessage!!, { addingMessage = it }, readOnly = isAdding, singleLine = true)
//                    Spacer(Modifier.width(2.dp))
//                    IconButton({
//                        isAdding = true
//                    }, enabled = !isAdding){
//                        Icon(Icons.Default.CheckCircle, "Add")
//                    }
//
//                    if(isAdding){
//                        LaunchedEffect(addingMessage, isAdding){
//                            addTodo(addingMessage!!, auth)
//                            isAdding = false
//                            addingMessage = null
//                        }
//                    }
//                }
//            }
//
//
//        }
//
//    }
//}

//@Composable
//fun DisplayTodo(toDo: ToDo){
//    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)){
//        Text(toDo.date.toLocalDateTime(TimeZone.currentSystemDefault()).toString())
//        Spacer(Modifier.width(5.dp))
//        Text(toDo.message)
//    }
//}