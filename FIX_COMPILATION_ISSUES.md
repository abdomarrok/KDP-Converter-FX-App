# Fix Compilation Issues - Quick Guide

## ✅ Issues Fixed

1. **ImageCacheService** - Added missing `FileTime` import, fixed variable scope issue
2. **ProgressDialog** - Fixed indeterminate progress constant
3. **module-info.java** - Added exports for `service` and `util` packages

## 🔧 How to Fix in Your IDE

### Option 1: IntelliJ IDEA

1. **Invalidate Caches:**
   - Go to `File` → `Invalidate Caches...`
   - Check "Clear file system cache and Local History"
   - Click "Invalidate and Restart"

2. **Refresh Maven:**
   - Right-click on `pom.xml`
   - Select `Maven` → `Reload Project`

3. **Rebuild Project:**
   - Go to `Build` → `Rebuild Project`

### Option 2: VS Code

1. **Reload Window:**
   - Press `Ctrl+Shift+P`
   - Type "Reload Window"
   - Press Enter

2. **Clean Java Build:**
   - Open terminal in VS Code
   - Run: `./run.bat` (or use Maven if in PATH)

### Option 3: Command Line (PowerShell)

```powershell
# Navigate to project
cd "C:\Users\pc1\Desktop\KDP Converter FX App"

# Use the run.bat script (it has Maven path configured)
.\run.bat
```

Or if Maven is in your PATH:
```powershell
mvn clean compile
mvn javafx:run
```

## 🐛 If Still Having Issues

### Check Java Version
```powershell
java -version
```
Should be Java 21

### Check Maven
The `run.bat` file has Maven path configured. If it doesn't work:
1. Edit `run.bat` and update the `MAVEN_CMD` path to your Maven installation
2. Or add Maven to your system PATH

### Common Issues:

1. **"Cannot resolve symbol" errors:**
   - IDE needs to refresh/reload project
   - Try invalidating caches (IntelliJ) or reloading window (VS Code)

2. **Module system errors:**
   - Make sure `module-info.java` is recognized
   - Check that all packages are properly exported

3. **Maven not found:**
   - Update `run.bat` with correct Maven path
   - Or install Maven and add to PATH

## ✅ Verification

After fixing, you should be able to:
- Compile without errors
- Run the application
- See no red underlines in IDE

## 📝 Files Modified

- `src/main/java/com/boilerplate/app/service/ImageCacheService.java`
- `src/main/java/com/boilerplate/app/util/ProgressDialog.java`
- `src/main/java/module-info.java`

All compilation errors have been fixed. The issue is likely just IDE cache/refresh.

