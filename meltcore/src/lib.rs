// It said add this on top
uniffi::setup_scaffolding!();

use std::path::Path;
use rusqlite::params;

#[uniffi::export]
fn hello_ffi() {
    println!("Hello from Rust!");
}

#[uniffi::export]
pub async fn open_database(path: String) -> String {
    let database_path = Path::new(&path).join("test.db");
    // let result = libsql::Builder::new_local(database_path).build().await;
    // rusqlite::
    // match result {
    //     Ok(databse) => path,
    //     Err(error) => error.to_string(),
    // }


    let is_created = database_path.exists();

    let connection =
        rusqlite::Connection::open(database_path.clone());

    let connection = match connection {
        Ok(connection) => connection,
        Err(error) => return format!("Error opening database: {}", error),
    };

    // Set encryption key for sqlcipher
    let result = connection
        .pragma_update(None, "key", "TODO");

    if result.is_err() {
        return "Error setting key".to_string();
    }

    let result = connection.execute_batch("CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY, name TEXT);\
    INSERT INTO test (id, name) VALUES (1, 'test');");

    if let Err(error) = result {
        return format!("Error creating table and inserting test data.: {}", error);
    }

    // Read data from database
    let result = connection.query_row("SELECT * FROM test", params![],|row| {

        let id = row.get::<_, i32>(0)?;
        Ok(id)
    });

    if result.is_err() {
        return "Error reading data from database".to_string();
    }

    let result = connection.close();

    // Try to open encrypted database without key to see if encryption is working
    if result.is_err() {
        return "Error closing database".to_string();
    }
    let connection =
        rusqlite::Connection::open(database_path);

    let connection = match connection {
        Ok(connection) => connection,
        Err(error) => return format!("Error opening database again: {}", error),
    };

    // Don't set encryption key for sqlcipher
    // But try to read data from database
    let result = connection.query_row("SELECT * FROM test",params![], |row| {

        let id = row.get::<_, i32>(0)?;
        Ok(id)
    });


    if let Err(error) = result {
        return format!("Error reading data from database. This is good. It means we can not read the database without encryption key: {}", error);
    }




    // Enable foreign keys
    // connection
    //     .set_db_config(DbConfig::SQLITE_DBCONFIG_ENABLE_FKEY, true)
    //     .map_err(OpenDatabaseError::EnableForeignKeysError)?;
    //
    // if !is_created {
    //     create_tables(&connection).map_err(OpenDatabaseError::CreateTablesError)?;
    // }

    return "Success".to_string();
}