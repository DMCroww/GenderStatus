# API Guide

**Current URL of the API:**<br>
*https://api.dmcroww.tech/genderStatus/v2/*<br>
***Accepts only POST JSON requests***<br>

## Required Fields for all requests:
- **`username`** : *String*<br>
  min 2, max 255 long

- **`password`** : *String*<br>
  min 8, max 255 long<br>
  defaults to `123456789`

- **`action`** : *String*<br>
  Space separated keywords for desired action

<br>

### API responds with JSON encoded object with following properties:
- **`success`** : *boolean*<br>
  signifies a successfull action
- **`code`** : *int*<br>
  error code, defaults to `0` for no error
- **`data`** : *array | object | string*<br>
  in case of an error, contains a string, otherwise as described [here](#types-of-response-data-property)

<br>

### All possible properties of request JSON object:

- `status` : [*Status object*](#the-status-object)

- `friend` : *string*<br>
  username of the friend the user is trying to add/remove/query

- `newPass` : *string*<br>
  8 - 255 characters long (inclusive)<br>
  must contain at least one letter and one number

- `nickname` : *string*<br>
  2 - 255 characters long (inclusive)

<br>

### Types of response 'data' property:
value type varies depending on the request, in case of *`success:false`* returns string with error message

#### *`typeof data == object`* for following request actions:

- `login`:  [*Status object*](#the-status-object)
- `get status`:  [*Status object*](#the-status-object)

#### *`typeof data == array`* for following request actions:

- `get history`: *array<*[*Status object*](#the-status-object)*>*
- `get friend history`: *array<*[*Status object*](#the-status-object)*>*
- `get friend requests`: *array\<object>*
- `get friends`: *array\<object>*
- `get avatars`: *array\<string>*
- `get backgrounds`: *array\<string>*

#### *`typeof data == string`* for following request actions:

- any `post ...` action

<br>

# 0. LOGIN `login`

Returns current stored user activity on success,
returns an error on failure "user not found/bad password"

<br>

# 1. `get` Actions

## 1.1 Status: `get status`
Returns currently set [Status Obj](#the-status-object) 

## 1.2 History: `get history`

Returns an array of [Status Obj](#the-status-object) 
Max 100 entries

## 1.3 Friends: `get friends`

returns array of Objects with following properties:
- `username` : *string* - friends username
- `nickname` : *string* - friends nickname
- `status` : *[Status Object](#the-status-obj)*

## 1.3 Specific Friend History: `get friend history [all]`

Returns an array of [Status Obj](#the-status-object) 
Max 30 entries (300 for `all`)

**Required Fields:**
- `friend`: *string*
username of friend you're trying to get

## 1.4 Friend requests `get friend requests`

returns aray Object of users that requested friendship if there are any, empty array otherwise
Each object has the following:
- `username`: *string* - username of the requestee
- `nickname`: *string* - Nickname of the requestee
- `friends`: *array* - mutual friends if any, empty otherwise

## 1.5 Avatars `get avatars`

Returns array of file paths of available avatars to use. 
Filtered only for specified user, does not include custom avatars of others

## 1.6 Backgrounds `get backgrounds`

returns array of strings (file paths) of backgrounds.


<br>
<br>
<br>

# 2. `post` Actions

### 2.1 Update User Status `post status`

**Required fields:**
- `status` : [Status Obj](#the-status-object)

## 2.2 Friend requests `post friend [add|remove]`

**Required fields:**
- `friend`: *string*<br>
  Friend's username

## 2.4 Change Password `post newnickname`

**Required fields:**
- `nickname`: *string*<br>
  New user nickname<br>
  2-255 characters


## 2.3 Change Password `post newpassword`

**Required fields:**
- `newPass`: *string*<br>
  New user password<br>
  8-255 characters<br>
  Must contain at least one letter and one number

<br>
<br>
<br>


## The `Status Object`:

- `id`: *int*<br>
  unique ID of the status
  
- `username`: *string*<br>
  username of the user who posted this status

- `avatar`: *string*<br>
  A uri path in the `https://api.dmcroww.tech/genderStatus/v2/avatars/` subdirectory pointing to the avatar image the user has selected<br>
  Can be `/default/<filename>` for default avatars, or `/<username>/<filename>` for personal avatars

- `activity`: *string*

- `mood`: *string*

- `sus`: *int*<br>
  `0-10` <br>
  used to calculate percentage in steps of 10 (i.e.: 0%, 10%, 60%)

- `gender`: *int*<br>
  Index of following array: `[null, "hyper masc", "masculine", "neutral masc", "neutral", "neutral fem", "feminine", "hyper fem"]`<br>
  `1-7` denoting current gender (mostly for genderfluid ppl...)

- `age`: *int*<br>
  Index of following array: `[null, "Embryo", "Baby", "Kid", "Teen", "Adult", "Oldge", "Deadge"]`<br>
  `1-7` denoting current mental "age"

- `visibleTo`: *array\<string>*<br>
  Array of usernames the status should be visible to<br>
  If left empty, status is considered public (seen by every friend, even new ones)<br>
  If a friend gets removed and then added back, the status will still be visible to them if they were included previously

- `timestamp`: *int*<br>
  UNIX timestamp (seconds) of last change
  