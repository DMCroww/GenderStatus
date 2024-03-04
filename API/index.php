<?php
/**
 * **Finish the operations, print JSON string and die()**
 * - Prints out a JSON-encoded string containing "success" flag and "data" property.
 * - If `$success = false`, sets HTTP response code to $code.
 * @param bool $success Boolean flag representing succcess of the operations.
 * @param mixed $data Data to be included in the response.
 * @param int $code HTTP response code (default is 400).
 * @return void
 */
function finish($success, $data, $code = 0): void {
	die(json_encode(array('success' => $success, 'code' => $code, 'data' => $data)));
}

/**
 * Modifies an object by selectively keeping or removing specified properties.
 *
 * This function takes an object representing user data, an array of property names, and a boolean
 * parameter to determine whether to keep or remove the specified properties. It creates a new object
 * by cloning the original and either removes the specified properties (default behavior) or keeps
 * only those properties, based on the value of the $keepProperties parameter. If a property does not
 * exist in the original object, it is ignored.
 *
 * @param array $data The original assoc array containing data.
 * @param array $propertyArr An array of property names to be selectively kept or removed.
 * @param bool $removeProperties If true, removes the specified properties (default); if false, keeps them.
 * @return array The modified associative array based on the specified properties.
 */
function cleaner(array|object $data, array $propertyArr, bool $removeProperties = true) {
	// Clone the original object to avoid modifying it directly
	$cleaned = array(...$data);

	foreach ($cleaned as $property => $value)
		if (
			(!$removeProperties && !in_array($property, $propertyArr)) ||
			($removeProperties && in_array($property, $propertyArr))
		)
			unset($cleaned[$property]);

	return $cleaned;
}

/**
 * Logs debug information to a file, organized by client IP address and timestamp.
 *
 * This function takes the provided data and logs it to a file in the './log/' directory. The log
 * file is named based on the client's IP address, and each entry includes a timestamp, formatted
 * date and time, and the provided data. If the log file for the client does not exist, it is
 * created; otherwise, the new entry is appended to the existing log file.
 *
 * @param string|object|array $data Debug information to be logged.
 */
function debugLog(string|object|array $input) {
	// Get the current timestamp, formatted date and time, and client IP address
	$timestamp = time();
	$time = date("d.m.Y H:i:s", $timestamp);
	$ip = $_SERVER['REMOTE_ADDR'];

	// Construct the log entry
	$data = json_encode($input);
	$logEntry = "$timestamp\n$time\n$data";

	// Check if the log file for the client exists, 
	// append if does, create new one if it doesnt.
	if (file_exists("./log/$ip.json"))
		fwrite(fopen("./log/$ip.json", "a+"), "\n\n$logEntry");
	else
		file_put_contents("./log/$ip.json", $logEntry);
}

/**
 * **Process incoming request and verify user login info**
 * 
 * Processes incoming POST requests, validates JSON format, and extracts required properties.
 * This function checks if the request method is POST and processes the incoming JSON data. Validates required properties 'username', 'password', and 'action'. If any validation fails, it terminates script execution with an appropriate error message.
 *
 * @return object An object representing the validated and sanitized data from the request.
 */
function processRequest() {
	global $conn;
	// Check if the request method is POST
	if ($_SERVER['REQUEST_METHOD'] !== 'POST')
		finish(false, "Only JSON POST requests allowed.", 10);

	// Attempt to retrieve and decode JSON data from the request
	if (!$json_data = file_get_contents('php://input'))
		finish(false, "No data received.", 10);
	if (!$data = json_decode($json_data, true))
		finish(false, "Bad JSON format.", 10);

	// Validate the presence of essential properties in the JSON data
	if (!isset($data['username']))
		finish(false, "Username missing.", 11);
	if (!isset($data['password']))
		finish(false, "Password missing.", 11);
	if (!isset($data['action']))
		finish(false, "Action missing.", 11);

	$u = strtolower($data['username']);
	$p = $data['password'];

	if (!$result = mysqli_query($conn, "SELECT * FROM users WHERE username = '$u'"))
		finish(false, "User not found.", 30);

	if (!$user = mysqli_fetch_assoc($result))
		finish(false, "User not found.", 30);

	if ($user['password'] != $p)
		finish(false, "Wrong password.", 12);

	// Return the sanitized data as an object
	return (object) array(...$user, ...$data);
}

/**
 * **Posts a new status into the DB**
 * - checks available columns of the DB, and sets them approprietly
 * - skips non-present columns to prevent 'column doesnt exist' errors
 */
function setStatus() {
	global $conn, $postData;
	$status = (array) $postData->status;

	if (count($status['visibleTo'])) {
		$visibleTo = $status['visibleTo'];
		$status['visibleTo'] = "";
		foreach ($$visibleTo as $key => $value)
			$status['visibleTo'] .= "|$value|";
	} else
		unset($status['visibleTo']);

	// Get the table columns dynamically
	$columns_query = mysqli_query($conn, "SHOW COLUMNS FROM statuses");
	while ($row = mysqli_fetch_assoc($columns_query))
		$table_columns[] = $row['Field'];

	// Prepare the SQL statement
	$keys = array('username');
	$values = array($postData->username);
	foreach ($status as $key => $value)
		if (in_array($key, $table_columns)) {
			$keys[] = "'$key'";
			$escaped = mysqli_real_escape_string($conn, $value);
			$values[] = "'$escaped'";
		}

	$k = implode(', ', $keys);
	$v = implode(', ', $values);

	if (mysqli_query($conn, "INSERT INTO your_table ($k) VALUES ($v)") === false)
		finish(false, "Database error: " . mysqli_error($conn), 1);

	finish(true, "Status updated.");
}

function setNickname() {
	global $conn, $postData;

	$username = $postData->username;
	$newnick = trim(preg_replace("/\s+/", " ", $postData->nickname));

	if (strlen($newnick) > 255)
		finish(false, "Nickname is too long.", 13);
	if (strlen($newnick) < 2)
		finish(false, "Nickname is too short.", 13);

	$n = mysqli_real_escape_string($conn, $newnick);
	$u = mysqli_real_escape_string($conn, $username);

	if (mysqli_query($conn, "UPDATE users SET nickname = '$n' WHERE username = '$u'") === false)
		finish(false, "Database error: " . mysqli_error($conn), 1);

	finish(true, "Nickname changed.");
}

function setPassword() {
	global $conn, $postData;

	$username = $postData->username;
	$newpass = $postData->newPass;

	// check for simplicity and length
	if ((bool) preg_match('/^(?=.*[[:alpha:]])(?=.*\d).+$/', $newpass) == false)
		finish(false, "Password too simple.", 13);
	if (strlen($newpass) > 255)
		finish(false, "Password is too long. Max 255 characters.", 13);
	if (strlen($newpass) < 8)
		finish(false, "Password is too short. Min 8 characters.", 13);

	$p = mysqli_real_escape_string($conn, $newpass);
	$u = mysqli_real_escape_string($conn, $username);

	if (mysqli_query($conn, "UPDATE users SET password = '$p' WHERE username = '$u'") === false)
		finish(false, "Database error: " . mysqli_error($conn), 1);

	finish(true, "Password changed.");
}

function getStatus(int $limit) {
	global $conn, $postData;

	$username = $postData->username;
	$sql = "SELECT * FROM statuses WHERE username = '$username' ORDER BY timestamp DESC LIMIT $limit";
	$result = mysqli_query($conn, $sql);
	if ($result === false)
		finish(false, "Database error: " . mysqli_error($conn), 1);

	finish(true, mysqli_fetch_all($result, MYSQLI_ASSOC));
}


/**
 * Function to get the last status of each friend that is visible to the user
 * @return void
 */
function getFriends() {
	global $conn, $postData;
	$u = $postData->username;
	$q = "SELECT s.* FROM (
       SELECT s.*, ROW_NUMBER() 
       OVER (PARTITION BY s.username ORDER BY s.id DESC) AS row_num
        FROM statuses s WHERE s.visibleTo = '' OR s.visibleTo LIKE '%|$u|%'
      ) AS s JOIN (
       SELECT TRIM(BOTH '|' FROM SUBSTRING_INDEX(SUBSTRING_INDEX(u.friends, '|', numbers.n), '|', -1)) AS friend FROM (
       SELECT @row := @row + 1 AS n
         FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) AS t1
         CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) AS t2
         CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) AS t3
         CROSS JOIN (SELECT @row := 0) AS r
       ) AS numbers JOIN users u ON LENGTH(u.friends) - LENGTH(REPLACE(u.friends, '|', '')) >= numbers.n - 1 WHERE u.username = '$u'
      ) AS friends ON s.username = friends.friend WHERE s.row_num = 1 ORDER BY s.id DESC;";

	$r1 = mysqli_query($conn, $q);
	$r2 = mysqli_query($conn, "SELECT * FROM users WHERE friends LIKE '%|$u|%'");
	if ($r1 === false || $r2 === false)
		finish(false, "Database error.", 1);

	while ($row = mysqli_fetch_assoc($r1))
		$statuses[$row['username']] = cleaner($row, ["id", "username", "visibleTo", "row_num"]);

	while ($row = mysqli_fetch_assoc($r2)) {
		$data[$row['username']] = cleaner($row, ['username', 'nickname'], false);
		if (isset($statuses[$row['username']]))
			$data[$row['username']]['status'] = $statuses[$row['username']];
	}

	finish(true, array_values($data));
}

function getFriendHistory(bool $all = false) {
	global $conn, $postData;

	$u = $postData->username;
	$f = $postData->friend;
	$q = "SELECT * FROM genderStatuses WHERE username = $f AND (visibleTo LIKE '%|$u|%' OR visibleTo = '')";

	if (!$all)
		$sql .= " LIMIT 30";

	$result = mysqli_query($conn, $q);
	if ($result === false)
		finish(false, "Database error." . mysqli_error($conn), 1);

	finish(true, mysqli_fetch_all($result, MYSQLI_ASSOC));
}

function getRequests() {
	global $conn, $postData;

	$username = $postData->username;

	$result = mysqli_query($conn, "SELECT * FROM users WHERE requests LIKE '%|$username|%'");
	if ($result === false)
		finish(false, "Database error.", 1);

	$requests = array();
	$userFriends = explode(" ", str_replace("  ", " ", str_replace("|", " ", $postData->friends)));
	while ($row = mysqli_fetch_assoc($result))
		$requests[] = array("username" => $row['username'], "nickname" => $row['nickname'], "friends" => array_intersect(explode(" ", str_replace("  ", " ", str_replace("|", " ", $row["friends"]))), $userFriends));

	finish(true, array_values($requests));
}

/**
 * **Add a friend request or confirm friendship if not present yet**
 * - Uses global $postData->friend
 * - Checks if the requesting user and the requested friend exist, then verifies if a friend request already exists or if they are already friends. If not, it adds the request or confirms the friendship, updates both user objects, and saves the changes to the database.
 */
function addFriend() {
	global $conn, $postData;

	$username = $postData->username;
	$friendUsername = $postData->friend;

	if (strpos($postData->friends, "|$friendUsername|") !== false)
		finish(false, "Already friends!", 14);

	if (strpos($postData->requests, "|$friendUsername|") !== false)
		finish(false, "Friend request pending!", 14);

	if (mysqli_fetch_assoc(mysqli_query($conn, "SELECT * FROM users WHERE username = '$friendUsername' AND requests LIKE '%|$username|%'"))) {
		// User found and already requested, confirm request
		$q1 = "UPDATE users SET friends = CONCAT(friends,'|$friendUsername|') WHERE username = '$username'";
		$q2 = "UPDATE users SET friends = CONCAT(friends,'|$username|'), requests = REPLACE(requests, '|$username|', '') WHERE username = '$friendUsername'";

		if (mysqli_query($conn, $q1) === false || mysqli_query($conn, $q2) === false)
			finish(false, "Database error.", 1);

		finish(true, "Friend added successfully.");

	} else if (mysqli_fetch_assoc(mysqli_query($conn, "SELECT * FROM users WHERE username = '$friendUsername'"))) {
		// user found, add to requests
		if (mysqli_query($conn, "UPDATE users SET requests = CONCAT(requests,'|$friendUsername|') WHERE username = '$username'") === false)
			finish(false, "Database error.", 1);

		finish(true, "Friend request set.");
	}

	finish(false, "User not found.", 30);
}

function removeFriend() {
	global $conn, $postData;

	$username = $postData->username;
	$friendUsername = $postData->friend;

	$q1 = "UPDATE users SET friends = REPLACE(requests, '|$username|', ''), requests = REPLACE(requests, '|$username|', '') WHERE username = '$friendUsername'";
	$q2 = "UPDATE users SET friends = REPLACE(requests, '|$friendUsername|', ''), requests = REPLACE(requests, '|$friendUsername|', '') WHERE username = '$username'";

	if (mysqli_query($conn, $q1) === false || mysqli_query($conn, $q2) === false)
		finish(false, "Database error.", 1);

	if (strpos($postData->friends, "|$friendUsername|") !== false)
		finish(true, "Friend removed.");

	finish(true, "Friend request removed.");
}

/**
 * **Get array of available avatars for user**
 * - Gets default avatars, checks for personalized avatars and if any, gets those as well.
 */
function getAvatars() {
	global $postData;
	$defaultAvatars = (array) array_diff(scandir("./avatars/default/"), array('.', '..'));

	$customAvatars = file_exists("./avatars/$postData->username/")
		? (array) array_diff(scandir("./avatars/$postData->username/"), array('.', '..'))
		: array();

	foreach ($defaultAvatars as $avatar)
		$returnArray[] = "default/$avatar";

	foreach ($customAvatars as $avatar)
		$returnArray[] = "$postData->username/$avatar";

	finish(true, array_values($returnArray));
}

/**
 * **Get array of predefined backgrounds on the server**
 */
function getBackgrounds() {
	$backgrounds = array_values(array_diff(scandir('./backgrounds/'), array('.', '..')));
	finish(true, $backgrounds);
}

header("Access-Control-Allow-Headers: *");
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

require_once('twitchDbInit.php'); // Init SQL dataase, returns $conn

// Get POST data and verify validity
$postData = processRequest();

$actions = explode(" ", $postData->action);
$action = array_shift($actions);
$subaction = implode(" ", $actions);

switch ($action) {
	case 'login':
		getStatus(1);

	case 'get':
		switch ($subaction) {
			case 'status':
				getStatus(1);

			case 'history':
				getStatus(100);

			case 'friend history':
				getFriendHistory();

			case 'friend history all':
				getFriendHistory(true);

			case 'friend requests':
				getRequests();

			case 'friends':
				getFriends();

			case 'avatars':
				getAvatars();

			case 'backgrounds':
				getBackgrounds();

			default:
				finish(false, "Unknown action.", 12);
		}

	case 'post':
		switch ($subaction) {
			case 'status':
				setStatus();

			case 'friend add':
				addFriend();

			case 'friend remove':
				removeFriend();

			case 'newpassword':
				setPassword();

			case 'newnickname':
				setNickname();

			default:
				finish(false, "Unknown action.", 12);
		}

	default:
		finish(false, "Unknown action.", 12);
}
