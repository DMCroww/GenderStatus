# API Guide

**Current URL of the API:**
*https://api.dmcroww.live/genderStatus/v2/*

***Accepts only POST JSON requests***


## Required Fields for all requests:
- **'username'** : User's username
- **'password'** : User's password
- **'action'** : Space separated keywords for desired action

## API responds with JSON encoded data containing the following:
- **'success'**: [ true | false ]
- **'data'**: [ array | object | false ]
- **'error'**: [ string | false ]


## All possible properties of the user object:
- 'username' : (string)
  - Unique ID of user
- 'password' : (string)
  - User's password 
  - min 8, max 255 long
  - defaults to "123456789"
- 'nick' : (string)
  - Display name
- 'friends' : (array)
  - String Array of all friends the user has added.
- 'avatar' : (string)
  - media URI ie: "avatar-1.png"
- 'activity' : (string)
  - User's activity
- 'mood' : (string)
  - User's mood
- 'gender' : (int)
  - 0 signifying "unset"
  - range 1-7
  - 1-7 should select in-app from predefined array `arrayIdx + 1`
  - ie.: `["hyper-masc", "masculine", "neutral masc", "neutral", "neutral fem", "feminine", "hyper-fem"]`
- 'age' : (int)
  - 0 signifying "unset"
  - range 1-7
  - 1-7 should select in-app from predefined array `arrayIdx + 1`
  - ie.: `["embryo", "baby", "kid", "teen", "adult", "oldge", "deadge"]`
- 'sus' : (int)
  - 0 signifying "unset"
  - range 1-11
  - 1-11 should be used to get % `(sus - 1) * 10`
- 'timestamp' : (int)
  - UNIX timestamp (seconds) of last change
- 'history' (array)
  - Array of all previously set statuses of user
  - each only includes following properties: `nick, avatar, activity, mood, gender, age, sus`

# 0. LOGIN

`'action' : 'login'`

Returns current stored user activity on success,
401 on "user not found/bad password"

### `'fetch'` Examples:

<table>
<tr>
<th align="center">
<img width="420" height="1">
request:
</th>
<th align="center">
<img width="420" height="1">
response:
</th>
</tr>
<tr>
<td>
Wrong password:

```json
{
  "username": "devUser1",
  "password": "99999999",
  "action": "login"
}
```

</td>
<td>

```json
{
  "success": false,
  "data": false,
  "error": "Incorrect password."
}
```

</td>
</tr>
<tr>
<td>
Correct password

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "login"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": [
    "circuit.png",
    "flower.png",
    "smoke.png",
    "squares.png"
  ],
  "error": false
}
```

</td>
</tr>


</table>

# 1. Fetch Actions

## 1.1 Self

`'action' : 'fetch self'`

Returns currently set status info.

## 1.2 Specific Friend
`'action' : 'fetch friend [status|history]'`

**Required Additional Fields:**

'friend': (username of friend you're trying to get)


## 1.3 Friends

`'action' : 'fetch friends'`

returns Object of valid friends if there are any, 403 otherwise


## 1.4 Avatars
`'action' : 'fetch avatars'`

returns string array of available avatars to use.


## 1.5 Backgrounds
`'action' : 'fetch backgrounds'`

returns string array of backgrounds


### `'fetch'` Examples:
<table>
<tr>
<th align="center">
<img width="420" height="1">
request:
</th>
<th align="center">
<img width="420" height="1">
response:
</th>
</tr>
<tr>
<td>

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "fetch self"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": {
    "nick": "Dev User 1",
    "avatar": "test.png",
    "activity": "Test activity.",
    "mood": "Test mood.",
    "gender": "3",
    "age": "3",
    "sus": "1"
  },
  "error": false
}
```

</td>
</tr>
<tr>
<td>

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "fetch friends"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": {
    "devUser2": {
      "nick": "Dev User 2",
      "avatar": "test.png",
      "activity": "Test activity 2.",
      "mood": "Test mood 2.",
      "gender": "3",
      "age": "3",
      "sus": "1",
      "timestamp": "1702913994"
    }
  },
  "error": false
}
```

</td>
</tr>
<tr>
<td>

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "fetch backgrounds"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": [
    "circuit.png",
    "flower.png",
    "smoke.png",
    "squares.png"
  ],
  "error": false
}
```

</td>
</tr>


</table>






# 2. Post Actions

## 2.1 Update User Status
`'action' : 'post status'`

Update users status. 

- Only included fields are altered. 
- At least one field has to be present.

#### **Optional fields:**

**'nick'** : (string) 
- New nickname (Display name)

**'avatar'** : (string)
- media URI ie: "avatar-1.png"
- should be selected from result of 'fetch avatars'

**'activity'** : (string)
- New user's activity

**'mood'** : (string)
- New user's mood

**'gender'** : (int)
- range 1-7
- from 1-indexed in-app array `arrayIdx + 1`

**'age'** : (int)
- range 1-7
- from 1-indexed in-app array `arrayIdx + 1`

**'sus'** : (int)
- range 1-11
- results in 0% to 100% `(sus - 1) * 10`

## 2.2 Change Password
`'action' : 'post newpass'`

**Required fields:**

**'newPass'** : (string)
- New user password
- 8-255 characters
- Allowed only `A-Z`,`a-z`,`0-9` and `.-_`

## 2.3 Friend requests
`'action' : 'post friend [request|confirm|deny|remove]'`

**Required fields:**

**'friend'** : (string)
- Friend's username


### `'post'` Examples:
<table>
<tr>
<th align="center">
<img width="420" height="1">
request:
</th>
<th align="center">
<img width="420" height="1">
response:
</th>
</tr>
<tr>
<td>

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "post status",
  "nick": "Dev 1",
  "status": "Test status 1",
  "sus": 3
}
```

</td>
<td>

```json
{
  "success": true,
  "data": "Status updated.",
  "error": false
}
```

</td>
</tr>
<tr>
<td>

```json
{
  "username": "devUser1",
  "password": "123456789",
  "action": "post newpass",
  "newPass": "Bad/Password"
}
```

</td>
<td>

```json
{
  "success": false,
  "data": false,
  "error": "Password contains forbidden characters."
}
```

</td>
</tr>
<tr>
<td>

```json
{
  "username": "devUser2",
  "password": "123456789",
  "action": "post newpass",
  "newPass": ".Allowed-Password_3"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": "Password changed.",
  "error": false
}
```

</td>
</tr>
<tr>
<td>

```json
{
  "username": "devUser2",
  "password": ".Allowed-Password_3",
  "action": "post friend deny",
  "friend": "devUser1"
}
```

</td>
<td>

```json
{
  "success": true,
  "data": "Request denied.",
  "error": false
}
```

</td>
</tr>
</table>
