# Aplikasi Inventaris Barang
Aplikasi "Inventaris Barang" adalah sebuah aplikasi berbasis Java yang digunakan untuk mengelola inventaris barang yang ada. Aplikasi ini menggunakan SQLite sebagai database untuk menyimpan data barang dan dilengkapi dengan antarmuka pengguna berbasis JFrame untuk interaksi yang mudah.
## Pembuat
- Nama: Ferdhyan Dwi Rangga Saputra 
- NPM: 2210010171 



## Fitur Utama
- **Menambahkan, mengedit, dan menghapus data inventaris barang.**
- **Menampilkan data barang dalam list dengan status yang dapat diubah.**
- **Mendukung pencarian data barang berdasarkan nama.**
- **Impor dan ekspor data inventaris barang dalam format CSV.**

---

## Struktur Proyek dan Fitur

### 1. Database Connection 
Fitur ini menghubungkan aplikasi ke database SQLite, membuat tabel `barang` jika belum ada.
```java
private void initDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:inventaris.db");

            // SQL statement untuk membuat tabel 'barang' jika belum ada
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS "barang" (
                    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
                    "nama" TEXT UNIQUE,
                    "jumlah" INTEGER,
                    "kategori" TEXT,
                    "kondisi" TEXT,
                    "gambar_path" TEXT,
                    "tanggal_masuk" DATE
                );
            """;

            // Menjalankan SQL statement untuk membuat tabel
            connection.createStatement().execute(createTableSQL);
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat menghubungkan ke database
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage());
        }
    }
```
--- 
### 2. Menambah Inventaris Barang
Event pada tombol "Tambah" untuk menyimpan barang.
```
private void saveBarang() {
        try {
            // Mengambil data dari field input
            String nama = tfNama.getText();
            int jumlah = Integer.parseInt(tfJumlah.getText());
            String kategori = cbKategori.getSelectedItem().toString();
            String kondisi = rbBagus.isSelected() ? "Bagus" : rbCukup.isSelected() ? "Cukup" : "Rusak";
            String gambarPath = tfGambarPath.getText();
            java.util.Date tanggal = jDateChooser1.getDate();
            java.sql.Date sqlTanggal = new java.sql.Date(tanggal.getTime()); // Konversi tanggal ke java.sql.Date

            // Salin gambar ke direktori proyek
            File sourceFile = new File(gambarPath);
            File destDir = new File("images");
            if (!destDir.exists()) {
                destDir.mkdirs(); // Buat direktori jika belum ada
            }
            File destFile = new File(destDir, sourceFile.getName());
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Gunakan jalur relatif
            String relativePath = "images/" + sourceFile.getName();

            // SQL untuk menyisipkan atau memperbarui data barang
            String sql = """
                INSERT OR REPLACE INTO barang (nama, jumlah, kategori, kondisi, gambar_path, tanggal_masuk)
                VALUES (?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                // Mengatur nilai-nilai parameter dalam pernyataan yang telah dipersiapkan
                pstmt.setString(1, nama);
                pstmt.setInt(2, jumlah);
                pstmt.setString(3, kategori);
                pstmt.setString(4, kondisi);
                pstmt.setString(5, relativePath);
                pstmt.setDate(6, sqlTanggal);
                pstmt.executeUpdate(); // Menjalankan pernyataan SQL
                JOptionPane.showMessageDialog(this, "Barang berhasil disimpan!"); // Menampilkan pesan sukses
                loadBarangList(); // Memuat ulang daftar barang
            }
        } catch (SQLException | IOException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat menyimpan data
            JOptionPane.showMessageDialog(this, "Gagal menyimpan data: " + e.getMessage());
        }
    }

// Menambahkan ActionListener untuk tombol simpan
btnSimpan.addActionListener(e -> saveBarang());
```
---

### 3. Mengedit Inventaris Barang
Event pada tombol "Edit" untuk memperbarui agenda.
```
private void updateBarang() {
        try {
            // Mengambil data dari field input
            String namaBaru = tfNama.getText(); // Nama barang baru
            int jumlah = Integer.parseInt(tfJumlah.getText()); // Jumlah barang
            String kategori = cbKategori.getSelectedItem().toString(); // Kategori barang
            String kondisi = rbBagus.isSelected() ? "Bagus" : rbCukup.isSelected() ? "Cukup" : "Rusak"; // Kondisi barang
            String gambarPath = tfGambarPath.getText(); // Jalur gambar barang
            String namaLama = this.selectedBarang; // Nama barang lama

            // SQL untuk memperbarui data barang berdasarkan nama lama
            String sql = """
                UPDATE barang SET nama = ?, jumlah = ?, kategori = ?, kondisi = ?, gambar_path = ?
                WHERE nama = ?
            """;

            // Mempersiapkan pernyataan SQL untuk eksekusi
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                // Mengatur nilai-nilai parameter dalam pernyataan yang telah dipersiapkan
                pstmt.setString(1, namaBaru); // Nama barang baru
                pstmt.setInt(2, jumlah); // Jumlah barang
                pstmt.setString(3, kategori); // Kategori barang
                pstmt.setString(4, kondisi); // Kondisi barang
                pstmt.setString(5, gambarPath); // Jalur gambar barang
                pstmt.setString(6, namaLama); // Nama barang lama dalam klausa WHERE
                // Menjalankan pernyataan SQL
                pstmt.executeUpdate();
                // Menampilkan pesan sukses pembaruan data
                JOptionPane.showMessageDialog(this, "Data barang berhasil diperbarui!");
                // Memuat ulang daftar barang untuk memperbarui tampilan
                loadBarangList();
            }
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat memperbarui data
            JOptionPane.showMessageDialog(this, "Gagal memperbarui data: " + e.getMessage());
        }
    }

// Menambahkan ActionListener untuk tombol ubah
btnUbah.addActionListener(e -> updateBarang());
```
---

### 4. Menghapus Inventaris Barang
Event pada tombol "Hapus" untuk menghapus inventaris barang.
```
private void deleteBarang() {
        // Mengambil nama barang yang dipilih dari JList
        String selectedBarang = jList3.getSelectedValue();
        if (selectedBarang != null) {
            // Menampilkan dialog konfirmasi penghapusan
            int response = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus barang ini?", "Konfirmasi Penghapusan", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            // Jika pengguna mengonfirmasi penghapusan
            if (response == JOptionPane.YES_OPTION) {
                try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM barang WHERE nama = ?")) {
                    // Mengatur parameter query dengan nama barang yang dipilih
                    pstmt.setString(1, selectedBarang);
                    // Menjalankan pernyataan SQL untuk menghapus barang
                    pstmt.executeUpdate();
                    // Menampilkan pesan sukses penghapusan
                    JOptionPane.showMessageDialog(this, "Barang berhasil dihapus!");
                    // Memuat ulang daftar barang
                    loadBarangList();
                    // Mengosongkan field input
                    clearFields();
                } catch (SQLException e) {
                    // Menampilkan pesan dialog jika terjadi kesalahan saat menghapus data
                    JOptionPane.showMessageDialog(this, "Error deleting data: " + e.getMessage());
                }
            }
        }
    }

// Menambahkan ActionListener untuk tombol hapus
btnHapus.addActionListener(e -> deleteBarang());
```
---
### 5. Mengekspor Inventaris Barang
Event pada tombol "Ekspor" untuk mengekspor data inventaris barang.
```
private void exportData() {
        try (FileWriter writer = new FileWriter("barang.csv")) {
            // Query untuk mengambil semua data dari tabel 'barang'
            String query = "SELECT * FROM barang";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                // Menulis header kolom ke file CSV
                writer.write("ID,Nama,Jumlah,Kategori,Kondisi,Gambar Path,Tanggal Masuk\n");
                while (rs.next()) {
                    // Menulis data setiap baris ke file CSV
                    writer.write(rs.getInt("id") + ","); // ID barang
                    writer.write(rs.getString("nama") + ","); // Nama barang
                    writer.write(rs.getInt("jumlah") + ","); // Jumlah barang
                    writer.write(rs.getString("kategori") + ","); // Kategori barang
                    writer.write(rs.getString("kondisi") + ","); // Kondisi barang
                    writer.write(rs.getString("gambar_path") + ","); // Jalur gambar barang
                    writer.write(rs.getDate("tanggal_masuk") + "\n"); // Tanggal masuk barang
                }
            }
            // Menampilkan pesan sukses setelah data berhasil diekspor
            JOptionPane.showMessageDialog(this, "Data exported to barang.csv!");
        } catch (IOException | SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat mengekspor data
            JOptionPane.showMessageDialog(this, "Error exporting data: " + e.getMessage());
        }
    }

// Menambahkan ActionListener untuk tombol ekspor
btnEkspor.addActionListener(e -> exportData());
```
---
### 6. Mengimpor Inventaris Barang
Event pada tombol "Impor" untuk mengimpor data inventaris barang.
```
private void importData() {
        // Membuat instance JFileChooser untuk memilih file
        JFileChooser fileChooser = new JFileChooser();

        // Menampilkan dialog pemilihan file dan menunggu pengguna untuk memilih file
        int result = fileChooser.showOpenDialog(this);

        // Jika pengguna menyetujui (menekan tombol "Open")
        if (result == JFileChooser.APPROVE_OPTION) {
            // Mendapatkan file yang dipilih
            File file = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                // Melewati baris header
                br.readLine();
                while ((line = br.readLine()) != null) {
                    // Memisahkan setiap baris data menggunakan koma sebagai pemisah
                    String[] parts = line.split(",");
                    if (parts.length == 7) {
                        // Mengambil data dari setiap bagian
                        int id = Integer.parseInt(parts[0]);
                        String nama = parts[1];
                        int jumlah = Integer.parseInt(parts[2]);
                        String kategori = parts[3];
                        String kondisi = parts[4];
                        String gambarPath = parts[5];
                        Date tanggalMasuk = java.sql.Date.valueOf(parts[6]);

                        // SQL untuk menyisipkan atau menggantikan data barang
                        String sql = """
                            INSERT OR REPLACE INTO barang (id, nama, jumlah, kategori, kondisi, gambar_path, tanggal_masuk)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        """;
                        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                            // Mengatur nilai-nilai parameter dalam pernyataan yang telah dipersiapkan
                            pstmt.setInt(1, id);
                            pstmt.setString(2, nama);
                            pstmt.setInt(3, jumlah);
                            pstmt.setString(4, kategori);
                            pstmt.setString(5, kondisi);
                            pstmt.setString(6, gambarPath);
                            pstmt.setDate(7, new java.sql.Date(tanggalMasuk.getTime()));
                            // Menjalankan pernyataan SQL
                            pstmt.executeUpdate();
                        }
                    }
                }
                // Memuat ulang daftar barang setelah impor data selesai
                loadBarangList();
                // Menampilkan pesan sukses impor data
                JOptionPane.showMessageDialog(this, "Data berhasil diimpor!");
            } catch (IOException | SQLException e) {
                // Menampilkan pesan dialog jika terjadi kesalahan saat mengimpor data
                JOptionPane.showMessageDialog(this, "Error importing data: " + e.getMessage());
            }
        }
    }

// Menambahkan ActionListener untuk tombol impor
btnImpor.addActionListener(e -> importData());
```
---
### 7. Mengambil Gambar Inventaris Barang
Event pada tombol "Ambil" untuk mengambil gambar inventaris barang.
```
private void pilihGambar() {
        // Membuat instance JFileChooser untuk memilih file
        JFileChooser fileChooser = new JFileChooser();

        // Menampilkan dialog pemilihan file dan menunggu pengguna untuk memilih file
        int result = fileChooser.showOpenDialog(this);

        // Jika pengguna menyetujui (menekan tombol "Open")
        if (result == JFileChooser.APPROVE_OPTION) {
            // Mendapatkan file yang dipilih
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Menyalin gambar ke direktori proyek
                File destDir = new File("images");
                if (!destDir.exists()) {
                    // Membuat direktori 'images' jika belum ada
                    destDir.mkdirs();
                }
                // Membuat file tujuan di direktori 'images' dengan nama yang sama dengan file sumber
                File destFile = new File(destDir, selectedFile.getName());
                // Menyalin file gambar ke direktori proyek
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Set jalur relatif gambar
                String relativePath = "images/" + selectedFile.getName();
                tfGambarPath.setText(relativePath);

                // Tampilkan gambar di JLabel
                ImageIcon originalIcon = new ImageIcon(relativePath);
                Image originalImage = originalIcon.getImage();

                // Set lebar dan tinggi spesifik gambar dalam piksel
                int width = 450; // lebar gambar dalam piksel
                int height = 225; // tinggi gambar dalam piksel

                // Ubah ukuran gambar agar sesuai dengan JLabel
                Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                lblGambar.setIcon(new ImageIcon(scaledImage));
            } catch (IOException ex) {
                // Menampilkan pesan dialog jika terjadi kesalahan saat menyalin file
                JOptionPane.showMessageDialog(this, "Error copying file: " + ex.getMessage());
            }
        }
    }

// Menambahkan ActionListener untuk tombol pilih gambar
btnGambar.addActionListener(e -> pilihGambar());
```
---
### 8. Mencari Inventaris Barang
Event pada tombol "Cari" untuk mencari inventaris barang yang diinginkan.
```
private void searchBarang() {
        // Mengambil kata kunci pencarian dari text field
        String keyword = tfCari.getText();
        // Membuat model daftar untuk menyimpan hasil pencarian
        DefaultListModel<String> model = new DefaultListModel<>();
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT nama FROM barang WHERE nama LIKE ?")) {
            // Mengatur parameter query dengan kata kunci pencarian, menggunakan wildcard '%' untuk pencarian sebagian
            pstmt.setString(1, "%" + keyword + "%");
            // Mengeksekusi query dan mendapatkan hasilnya
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Menambahkan setiap nama barang yang cocok dengan kata kunci ke model daftar
                model.addElement(rs.getString("nama"));
            }
            // Mengatur model daftar ke JList untuk menampilkan hasil pencarian
            jList3.setModel(model);
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat melakukan pencarian data
            JOptionPane.showMessageDialog(this, "Error searching data: " + e.getMessage());
        }
    }

// Menambahkan ActionListener untuk tombol cari
btnCari.addActionListener(e -> searchBarang());
```
---
## Teknologi yang Digunakan
- **Bahasa Pemrograman**: Java
- **Database**: SQLite
- **Library Eksternal**:
  - `sqlite-jdbc`: Untuk koneksi SQLite.
  - `com.toedter.calendar.JDateChooser`: Untuk memilih tanggal.

---

## Tampilan Aplikasi Saat di Run

![image](https://github.com/user-attachments/assets/0f570baa-421d-4f3c-812c-85f221ed7169)
