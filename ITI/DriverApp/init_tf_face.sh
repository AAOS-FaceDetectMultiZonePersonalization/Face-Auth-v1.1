#!/system/bin/sh

# Create the tf_face directory and images subfolder if missing
mkdir -p /data/system/tf_face/images

# Create DB files if they don't exist
for f in face_database.db face_database.db-shm face_database.db-wal; do
    if [ ! -f "/data/system/tf_face/$f" ]; then
        touch "/data/system/tf_face/$f"
    fi
done

# Set wide permissions (your app runs in user context)
chmod -R 0777 /data/system/tf_face

# Restore SELinux labels if required
restorecon -R /data/system/tf_face
