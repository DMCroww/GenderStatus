<?php
header("Access-Control-Allow-Headers: *");
header('Content-Type: application/json; charset=utf-8');

header('Cache-Control: no-cache, no-store, must-revalidate');
header('Pragma: no-cache');
header('Expires: 0');

require_once('twitchDbInit.php'); // Init SQL dataase, returns $conn


$data = processRequest(); // process & verify request input

if (!$storedUser = verifyUser($data))
	fail("Incorrect password.", 403);

$action = explode(" ", $data->action);

switch ($action[0]) {
	case 'login':
		success($storedUser);
	case 'fetch':
		if (!isset($action[1]))
			fail("Subaction missing.");
		switch ($action[1]) {
			case 'self':
				success(keepOnly($storedUser));

			case 'friend':
				if (!isset($action[2]))
					fail("Subaction missing.");
				switch ($action[2]) {
					case 'status':
						if (!$friendData = getFriend($data->username, $data->friend))
							fail("Not friends.", 403);
						success(keepOnly($friendData));

					case 'history':
						if (!$friendData = getFriend($data->username, $data->friend))
							fail("Not friends.", 403);
						success($friendData->history);

					default:
						fail("Unknown action.", 400);
				}
			case 'friends':
				success(fetchFriends($storedUser));
			case 'avatars':
				$customAvatars = [];
				$defaultAvatars = [];
				$defaultAvatars = array_diff(scandir("./avatars/default/"), array('.', '..'));
				if (file_exists("./avatars/$data->username/"))
					$customAvatars = array_diff(scandir("./avatars/$data->username/"), array('.', '..'));

				foreach ($customAvatars as $avatar)
					$returnArray[] = "$data->username/$avatar";

				foreach ($defaultAvatars as $avatar)
					$returnArray[] = "default/$avatar";

				success(array_values($returnArray));

			case 'backgrounds':
				success(array_values(array_diff(scandir('./backgrounds/'), array('.', '..'))));

			default:
				fail("Unknown action.", 400);
		}

	case 'post':
		if (!isset($action[1]))
			fail("Subaction missing.");
		switch ($action[1]) {
			case 'status':
				array_unshift($storedUser->history, keepOnly($storedUser, ['avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'visibleTo', 'timestamp']));


				$newData = keepOnly($data, ['username', 'nick', 'avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'visibleTo']);
				if (empty(get_object_vars(cleanUp($newData, ["username"]))))
					fail("Nothing to update.", 400);
				$newData->timestamp = time();
				$newData->history = $storedUser->history;

				if (!saveUsersDB($newData))
					fail("Database error.", 500);
				success("Status updated.");

			case 'newpass':
				if (preg_match('/^[A-Za-z0-9\.\-_]+$/', $data->newPass) === 0)
					fail("Password contains forbidden characters.", 403);
				if (strlen($data->newPass) > 255)
					fail("Password is too long.", 403);
				if (strlen($data->newPass) < 8)
					fail("Password is too short.", 403);

				$data->password = $data->newPass;
				$newData = keepOnly($data, ["username", "password"]);
				if (saveUsersDB($newData))
					success("Password changed.");
				else
					fail("Database error.", 500);

			case 'newnick':
				if (preg_match('/^[A-Za-z0-9\.\-_]+$/', $data->newPass) === 0)
					fail("Password contains forbidden characters.", 403);
				if (strlen($data->newPass) > 255)
					fail("Password is too long.", 403);
				if (strlen($data->newPass) < 8)
					fail("Password is too short.", 403);

				$data->password = $data->newPass;
				$newData = keepOnly($data, ["username", "nick"]);
				if (saveUsersDB($newData))
					success("Password changed.");
				else
					fail("Database error.", 500);

			case 'friend':
				if (!isset($action[2]))
					fail("Subaction missing.");
				$friendname = $data->friend;
				$friendData = keepOnly(getUser($friendname), ["username", "friends", "requests"]);
				$storedUser = keepOnly($storedUser, ["username", "friends", "requests"]);
				switch ($action[2]) {
					case 'add':
						addFriend($storedUser, $friendData);
						break;


					case 'remove':
						removeFriend($storedUser, $friendData);
						break;

					default:
						fail("Unknown action.", 400);
				}
			default:
				fail("Unknown action.");
		}
	default:
		fail("Unknown action.");
}


/* FUNCTIONS */ {
	/**
	 * Prints out a JSON-encoded string indicating a successful operation, including provided data, and dies.
	 *
	 * This function constructs a JSON response with a "success" property set to true, a "data" property
	 * containing the provided data, and an "error" property set to false. It then outputs the JSON
	 * string and terminates script execution.
	 *
	 * @param object|array|string $data Data to be included in the response.
	 */
	function success(object|array|string $data) {
		die(json_encode(array('success' => true, 'data' => $data, 'error' => false)));
	}
	/**
	 * Prints out a JSON-encoded string indicating a failed operation, including an error message.
	 *
	 * This function sets the HTTP response code to the provided code (default is 400), constructs a
	 * JSON response with a "success" property set to false, a "data" property set to false, and an
	 * "error" property containing the provided error message. It then outputs the JSON string and
	 * terminates script execution.
	 *
	 * @param string $message Error message to be included in the response.
	 * @param int $code HTTP response code (default is 400).
	 */
	function fail(string $message, int $code = 400) {
		http_response_code($code);
		die(json_encode(array('success' => false, 'data' => false, 'error' => $message)));
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
	 * Processes incoming POST requests, validates JSON format, and extracts required properties.
	 *
	 * This function checks if the request method is POST and processes the incoming JSON data. It
	 * ensures that the JSON data is received, correctly formatted, and contains essential properties
	 * such as 'username', 'password', and 'action'. If any validation fails, it terminates script
	 * execution with an appropriate error message.
	 *
	 * @return object An object representing the validated and sanitized data from the request.
	 */
	function processRequest() {
		// Check if the request method is POST
		if ($_SERVER['REQUEST_METHOD'] !== 'POST')
			fail("Only JSON POST requests allowed.", 400);

		// Attempt to retrieve and decode JSON data from the request
		if (!$json_data = file_get_contents('php://input'))
			fail("No data received.", 400);
		if (!$data = json_decode($json_data))
			fail("Bad JSON format.", 400);

		// Validate the presence of essential properties in the JSON data
		if (!property_exists($data, 'username'))
			fail("Username missing.", 400);
		if (!property_exists($data, 'password'))
			fail("Password missing.", 400);
		if (!property_exists($data, 'action'))
			fail("Action missing.", 400);

		// Return the sanitized data as an object
		return (object) $data;
	}
	/**
	 * Verifies user credentials and returns stored user data on success.
	 *
	 * This function takes user input data containing a username and password. It retrieves the user's
	 * data from the database using the provided username. If the provided password matches the stored
	 * password, the function returns an object containing the user data. If the passwords do not match,
	 * the function returns false.
	 *
	 * @param object $postData Input data containing username and password for verification.
	 * @return object|false An object containing user data from the database on successful verification,
	 *                      or false if the provided password is incorrect.
	 */
	function verifyUser(object $postData) {
		$user = getUser($postData->username);
		return ($postData->password == $user->password) ? (object) $user : false;
	}
	/**
	 * Retrieves user data from the database based on the provided username.
	 *
	 * This function queries the database to retrieve user data for the specified username. It
	 * sanitizes the input to prevent SQL injection using mysqli_real_escape_string. If the user
	 * is found, the function returns an object containing the user data. If the user is not found,
	 * it terminates script execution with a "User not found" error and a 404 HTTP response code.
	 *
	 * @param string $username The username for which to retrieve user data.
	 * @return object An object containing user data from the database.
	 */
	function getUser(string $username) {
		global $conn;

		$username = mysqli_real_escape_string($conn, $username); // Sanitize input to prevent SQL injection
		$sql = "SELECT * FROM genderUsers WHERE username = '$username'";

		if (!$result = mysqli_query($conn, $sql))
			fail("User not found.", 404);

		if (!$user = mysqli_fetch_object($result))
			fail("User not found.", 404);


		// Decode JSON columns
		$user->history = json_decode($user->history);
		$user->friends = json_decode($user->friends);
		$user->requests = json_decode($user->requests);
		$user->visibleTo = json_decode($user->visibleTo);

		return (object) $user;
	}
	/**
	 * Add a friend request or confirm friendship if not present yet
	 * 
	 * Checks if the requesting user and the requested friend exist, then verifies if a friend request already exists or if they are already friends. If not, it adds the request or confirms the friendship, updates both user objects, and saves the changes to the database.
	 * 
	 * @param object $storedUser The user object initiating the friend request
	 * @param object $friendData The user object receiving the friend request
	 * @return void
	 */
	function addFriend(object $storedUser, object $friendData) {
		$username = $storedUser->username;
		$friendname = $friendData->username;

		// check for double requests
		if (in_array($username, $storedUser->requests))
			$result = "Already requested.";
		else if (in_array($username, $storedUser->friends))
			$result = "Already friends.";
		else if (!in_array($username, $friendData->requests)) { // check if friend has requested
			array_push($storedUser->requests, $username); // set friend request
			$result = "Request added.";
		} else {
			array_push($storedUser->friends, $friendname); // if not in own list yet, add
			array_push($friendData->friends, $username);

			// remove from friend's requests list if present
			if (($key = array_search($username, $friendData->requests)) !== false)
				unset($friendData->requests[$key]);

			$result = "Friend added.";
		}
		if (!saveUsersDB($storedUser) || !saveUsersDB($friendData))
			fail("Database error.", 500);

		success($result);
	}

	/**
	 * Removes friend from $storedUser and saves the DB entry
	 * @param object $storedUser
	 * @param object $friendData
	 * @return never
	 */
	function removeFriend(object $storedUser, object $friendData) {
		$username = $storedUser->username;
		$friendname = $friendData->username;

		$key = array_search($username, $friendData->friends);
		$friendkey = array_search($friendname, $storedUser->friends);
		if ($key == false || $friendkey == false)
			fail("Not friends.", 403);

		unset($friendData->friends[$key]);
		unset($storedUser->friends[$friendkey]);

		if (!saveUsersDB($friendData) || !saveUsersDB($storedUser))
			fail("Database error.", 500);
		success("Friend removed.");
	}

	/**
	 * Verifies friendship between users and returns stored friend data on success.
	 *
	 * This function takes user's username and the target friend's username. It retrieves the
	 * friend's data from the database using the provided friend username. If the target user and the
	 * friend are friends (as indicated by the "friends" property in the friend's data), the function
	 * returns an object containing the friend's data. If the users are not friends, it returns false.
	 *
	 * @param string $username User's username.
	 * @param string $friendUsername Target friend's username.
	 * @return object|false An object containing friend data from the database on successful friendship
	 *                      verification, or false if the users are not friends.
	 */
	function getFriend(string $username, string $friendUsername) {
		// Retrieve friend data from the database based on the provided friend username
		$friend = getUser($friendUsername);

		// Check if the user and the friend are friends
		return (in_array($username, $friend->friends)) ? (object) $friend : false;
	}
	/**
	 * Fetches friends' statuses based on the provided user data.
	 *
	 * This function iterates through the friends in the provided user data and retrieves their
	 * statuses using the getFriend function. It then filters and keeps only specific properties
	 * in the retrieved data. The resulting array contains friends' statuses organized by username.
	 *
	 * @param object $data The user data containing the username and friends list.
	 * @return array|false An array containing friends' statuses or false if no statuses are available.
	 */
	function fetchFriends(object $data) {
		$username = $data->username;
		$returnList = [];

		foreach ($data->friends as $friend) {
			if ($friendData = getFriend($username, $friend))
				if (in_array($username, $friendData->visibleTo) || count($friendData->visibleTo) == 0)
					$returnList[] = keepOnly($friendData, ['username', 'avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'timestamp']);
				else
					for ($i = 0; $i < count($friendData->history); $i++) {
						if (property_exists($friendData->history[$i], "visibleTo")) {
							if (in_array($username, $friendData->history[$i]->visibleTo)) {
								$friendData->history[$i]->username = $friendData->username;
								$returnList[] = keepOnly($friendData->history[$i], ['username', 'avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'timestamp']);
								break;
							}
						} else {
							$friendData->history[$i]->username = $friendData->username;
							$returnList[] = keepOnly($friendData->history[$i], ['username', 'avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'timestamp']);
						}
					}
		}

		return (array) $returnList;
	}
	/**
	 * Removes specified properties from an object, creating a cleaned-up version.
	 *
	 * This function takes an object representing user data and an array of property names to be removed.
	 * It creates a new object by cloning the original and removes the specified properties. The cleaned-up
	 * object contains only the remaining properties relevant for status information. If a property to be
	 * removed does not exist in the original object, it is ignored.
	 *
	 * @param object $userData The original object containing user data.
	 * @param array $removeArr An array of property names to be removed from the object.
	 * @return object The cleaned-up object with only relevant status information.
	 */
	function cleanUp(object $userData, array $removeArr) {
		// Return the object with only the specified properties removed as a new object
		return (object) modifyObjectProperties($userData, $removeArr, false);
	}
	/**
	 * Keeps only specified properties in an object, removing all others.
	 *
	 * This function takes an object representing user data and an array of property names to be kept.
	 * It creates a new object by cloning the original and removes all properties that are not listed in
	 * the specified array. If a property does not exist in the original object, it is ignored.
	 *
	 * @param object $userData The original object containing user data.
	 * @param array $keepArr An array of property names to be kept in the object.
	 * @return object The object with only the specified properties.
	 */
	function keepOnly(object $userData, array $keepArr = ['avatar', 'activity', 'mood', 'gender', 'age', 'sus', 'timestamp', 'visibleTo']) {
		// Return the object with only the specified properties kept as a new object
		return (object) modifyObjectProperties($userData, $keepArr, true);
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
	 * @param object $data The original object containing user data.
	 * @param array $propertyArr An array of property names to be selectively kept or removed.
	 * @param bool $keepProperties If true, keeps only the specified properties; if false, removes them (default behavior).
	 * @return object The modified object based on the specified properties.
	 */
	function modifyObjectProperties(object $data, array $propertyArr, bool $keepProperties = false) {
		// Clone the original object to avoid modifying it directly
		$newObject = clone $data;

		// Determine whether to keep or remove the specified properties
		foreach ($newObject as $property => $value) {
			if (
				($keepProperties && !in_array($property, $propertyArr)) ||
				(!$keepProperties && in_array($property, $propertyArr))
			) {
				unset($newObject->$property);
			}
		}

		// Return the modified object as a new object
		return (object) $newObject;
	}
	/**
	 * Updates data in the database based on the provided object.
	 *
	 * This function takes an object representing user data and dynamically generates an SQL query to
	 * update the corresponding row in the database table "genderUsers" based on the object's properties.
	 * The SQL query includes a SET clause with sanitized values, and the update is targeted using the
	 * username column. The function returns the result of the mysqli_query operation, indicating the
	 * success or failure of the update.
	 *
	 * @param object $data The data to be updated in the database.
	 * @return bool|mysqli_result Returns true on success or false on failure of the update operation.
	 */
	function saveUsersDB(object $data) {
		global $conn;
		$username = $data->username;
		unset($data->username);

		if (isset($data->friends))
			$data->friends = json_encode($data->friends);
		if (isset($data->requests))
			$data->requests = json_encode($data->requests);

		$setClause = implode(
			', ',
			array_map(
				function ($property, $value) use ($conn) {
					return "`$property` = '" . mysqli_real_escape_string($conn, $value) . "'";
				},
				array_keys((array) $data),
				(array) $data
			)
		);

		$sql = "UPDATE genderUsers SET $setClause WHERE `username` = '$username'";

		return mysqli_query($conn, $sql);
	}

	/**
	 * Summary of saveStatusDB
	 * @param string $username
	 * @param object $status
	 * @return bool|mysqli_result
	 */
	function saveStatusDB(string $username, object $status) {
		global $conn;

		$setClause = implode(
			', ',
			array_map(
				function ($property, $value) use ($conn) {
					return "`$property` = '" . mysqli_real_escape_string($conn, $value) . "'";
				},
				array_keys((array) $status),
				(array) $status
			)
		);

		$sql = "UPDATE genderUsers SET $setClause WHERE `username` = '$username'";

		return mysqli_query($conn, $sql);
	}

	/**
	 * Get latests statuses visible to $username
	 * @param string $username
	 * @return bool|mysqli_result
	 */
	function getLatestStatuses(string $username) {
		global $conn;
		$sql = "WITH FirstRowPerUsername AS (SELECT username, MAX(id) AS max_id FROM genderStatuses WHERE visibleTo LIKE '%\"$username\"%' OR visibleTo = '[]' GROUP BY username) SELECT t.* FROM genderStatuses t INNER JOIN FirstRowPerUsername f ON t.username = f.username AND t.id = f.max_id ORDER BY t.username, t.id";
		return mysqli_query($conn, $sql);
	}

	/**
	 * Get a full status history of user "$friendUsername" thats visible to "$username"
	 * @param string $username
	 * @param string $friendUsername
	 * @return bool|mysqli_result
	 */
	function getStatusesHistory(string $username, string $friendUsername) {
		global $conn;
		$sql = "SELECT * FROM genderStatuses WHERE username = $friendUsername AND (visibleTo LIKE '%\"$username\"%' OR visibleTo = '[]')";
		return mysqli_query($conn, $sql);
	}
}