# API Examples:


## `'get'` Examples:

<br>

### action: `login`
```json
request: 
{
  "username": "devUser1",
  "password": "123456789",
  "action": "login"
}

response: 
{
  "success": true,
  "code": 0,
  "data": {
    "avatar": "test.png",
    "activity": "Test activity.",
    "mood": "Test mood.",
    "gender": 3,
    "age": 3,
    "sus": 1
  }
}
```
<br>

### action: `get friends`

```json
request:
{
  "username": "dmcroww",
  "password": "123456789",
  "action": "get friends"
}

response:
{
  "success": true,
  "code": 0,
  "data": [
    {
      "username": "devuser1",
      "nickname": "Dev User 1",
      "status": {
        "avatar": "/default/hackermans.png",
        "activity": "Test activity 2.",
        "mood": "Test mood 2.",
        "gender": 3,
        "age": 4,
        "sus": 8,
        "timestamp": "1702914500"
      }
    },
    {
      "username": "devuser2",
      "nickname": "Dev 2",
      "status": {
        "avatar": "/default/pepega.png",
        "activity": "Test activity.",
        "mood": "Test mood.",
        "gender": 4,
        "age": 2,
        "sus": 3,
        "timestamp": "1702913994"
      }
    }
  ]
}
```

<br>

###action `get backgrounds`

```json
request:
{
  "username": "devUser1",
  "password": "123456789",
  "action": "get backgrounds"
}

response:
{
  "success": true,
  "code": 0,
  "data": [
    "circuit.png",
    "flower.png",
    "smoke.png",
    "squares.png"
  ]
}
```

</td></tr>


</table>



## `'post'` Examples:

<br>


### action `post status`
```json
request:
{
  "username": "devUser1",
  "password": "123456789",
  "action": "post status",
  "status": {
    "avatar": "/default/hackermans.png",
    "activity": "Test activity 2.",
    "mood": "Test mood 2.",
    "gender": 3,
    "age": 4,
    "sus": 8,
	 "visibleTo":[]
  }  
}

response:
{
  "success": true,
  "data": "Status updated.",
  "code":0
}
```

<br>

### action `post newpassword`

```json
request:
{
  "username": "devUser1",
  "password": "123456789",
  "action": "post newpassword",
  "newPass": "ReallyBadPassword"
}

response:
{
  "success": false,
  "data": "Password too simple.",
  "code": 13
}
```

<br>

### action `post newpassword`

```json
request:
{
  "username": "devUser2",
  "password": "123456789",
  "action": "post newpassword",
  "newPass": ".Allowed-Password_3"
}

response:
{
  "success": true,
  "data": "Password changed.",
  "code":0
}
```

<br>

### action `post friend remove`

```json
request:
{
  "username": "devUser2",
  "password": ".Allowed-Password_3",
  "action": "post friend remove",
  "friend": "devUser1"
}

response:
{
  "success": true,
  "data": "Friend removed.",
  "code": 0
}
```

</td>
</tr>
</table>

