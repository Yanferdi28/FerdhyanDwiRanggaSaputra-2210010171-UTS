import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*; 
import java.sql.*; 
import javax.swing.*;
import java.awt.Image;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


/**
 *
 * @author Fetra
 */
public class InventarisBarangFrame extends javax.swing.JFrame {
    private Connection connection;

    public InventarisBarangFrame() {
        initDatabase();
        initComponents();
        loadCategories();
        loadBarangList();
        addListeners();
    }
    
    private void initDatabase() {
        try {
            // Menghubungkan ke database SQLite. Jika database tidak ada, maka akan dibuat.
            connection = DriverManager.getConnection("jdbc:sqlite:inventaris.db");

            // SQL statement untuk membuat tabel 'barang' jika belum ada.
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS barang (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, // ID unik untuk setiap barang, auto-increment
                    nama TEXT UNIQUE, // Nama barang, harus unik
                    jumlah INTEGER, // Jumlah barang
                    kategori TEXT, // Kategori barang
                    kondisi TEXT, // Kondisi barang (misal: Bagus, Rusak, dll.)
                    gambar_path TEXT, // Jalur penyimpanan gambar barang
                    tanggal_masuk DATE // Tanggal barang masuk ke inventaris
                );
            """;

            // Menjalankan SQL statement untuk membuat tabel.
            connection.createStatement().execute(createTableSQL);
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat menghubungkan ke database.
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage());
        }
    }


    private void loadCategories() {
        cbKategori.setModel(new DefaultComboBoxModel<>(new String[]{"Elektronik", "Furniture", "Alat Tulis", "Lainnya"}));
    }

    private void loadBarangList() {
        // Membuat model daftar untuk menyimpan nama-nama barang
        DefaultListModel<String> model = new DefaultListModel<>();
        try (Statement stmt = connection.createStatement()) {
            // Melakukan query untuk mengambil semua nama barang dari tabel 'barang'
            ResultSet rs = stmt.executeQuery("SELECT nama FROM barang");
            while (rs.next()) {
                // Menambahkan setiap nama barang ke model daftar
                model.addElement(rs.getString("nama"));
            }
            // Mengatur model daftar ke JList untuk ditampilkan
            jList3.setModel(model);
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat memuat data
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }


    private void showBarangDetails(String namaBarang) {
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM barang WHERE nama = ?")) {
            // Setel parameter query dengan nama barang yang diberikan
            pstmt.setString(1, namaBarang);

            // Eksekusi query dan ambil hasilnya
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Setel nilai field dari hasil query
                tfNama.setText(rs.getString("nama"));
                tfJumlah.setText(String.valueOf(rs.getInt("jumlah")));
                cbKategori.setSelectedItem(rs.getString("kategori"));

                // Setel radio button berdasarkan kondisi barang
                String kondisi = rs.getString("kondisi");
                rbBagus.setSelected("Bagus".equals(kondisi));
                rbCukup.setSelected("Cukup".equals(kondisi));
                rbRusak.setSelected("Rusak".equals(kondisi));

                // Setel jalur gambar dan tampilkan gambar di JLabel
                tfGambarPath.setText(rs.getString("gambar_path"));
                if (rs.getString("gambar_path") != null) {
                    ImageIcon originalIcon = new ImageIcon(rs.getString("gambar_path"));
                    Image originalImage = originalIcon.getImage();

                    // Set lebar dan tinggi spesifik gambar dalam piksel
                    int width = 450; // lebar gambar dalam piksel
                    int height = 225; // tinggi gambar dalam piksel

                    // Ubah ukuran gambar agar sesuai dengan JLabel
                    Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    lblGambar.setIcon(new ImageIcon(scaledImage));
                } else {
                    lblGambar.setIcon(null);
                }

                // Ambil data tanggal dan setel ke JDateChooser
                Date tanggalMasuk = rs.getDate("tanggal_masuk");
                jDateChooser1.setDate(tanggalMasuk);
            }
        } catch (SQLException e) {
            // Tampilkan pesan dialog jika terjadi kesalahan saat mengambil data
            JOptionPane.showMessageDialog(this, "Error retrieving data: " + e.getMessage());
        }
    }

    
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



    private void clearFields() {
        // Mengosongkan field nama
        tfNama.setText("");
        // Mengosongkan field jumlah
        tfJumlah.setText("");
        // Mengatur combobox kategori ke index pertama (kosongkan pilihan)
        cbKategori.setSelectedIndex(0);
        // Mengosongkan pilihan radio button kondisi barang
        bgKondisi.clearSelection(); 
        // Mengosongkan field jalur gambar
        tfGambarPath.setText("");
        // Menghapus ikon gambar yang ditampilkan
        lblGambar.setIcon(null);
        // Mengosongkan JDateChooser (tanggal masuk barang)
        jDateChooser1.setDate(null);
    }


    
    private void updateBarang() {
        try {
            // Mengambil data dari field input
            String nama = tfNama.getText();
            int jumlah = Integer.parseInt(tfJumlah.getText());
            String kategori = cbKategori.getSelectedItem().toString();
            String kondisi = rbBagus.isSelected() ? "Bagus" : rbCukup.isSelected() ? "Cukup" : "Rusak";
            String gambarPath = tfGambarPath.getText();

            // SQL untuk memperbarui data barang berdasarkan nama
            String sql = """
                UPDATE barang SET jumlah = ?, kategori = ?, kondisi = ?, gambar_path = ?
                WHERE nama = ?
            """;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                // Mengatur nilai-nilai parameter dalam pernyataan yang telah dipersiapkan
                pstmt.setInt(1, jumlah);
                pstmt.setString(2, kategori);
                pstmt.setString(3, kondisi);
                pstmt.setString(4, gambarPath);
                pstmt.setString(5, nama);
                // Menjalankan pernyataan SQL
                pstmt.executeUpdate();
                // Menampilkan pesan sukses pembaruan data
                JOptionPane.showMessageDialog(this, "Data barang berhasil diperbarui!");
                // Memuat ulang daftar barang
                loadBarangList();
            }
        } catch (SQLException e) {
            // Menampilkan pesan dialog jika terjadi kesalahan saat memperbarui data
            JOptionPane.showMessageDialog(this, "Gagal memperbarui data: " + e.getMessage());
        }
    }


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



    private void addListeners() {
        // Menambahkan ActionListener untuk tombol simpan
        btnSimpan.addActionListener(e -> saveBarang());
        // Menambahkan ActionListener untuk tombol hapus
        btnHapus.addActionListener(e -> deleteBarang());
        // Menambahkan ActionListener untuk tombol cari
        btnCari.addActionListener(e -> searchBarang());
        // Menambahkan ActionListener untuk tombol impor
        btnImpor.addActionListener(e -> importData());
        // Menambahkan ActionListener untuk tombol ekspor
        btnEkspor.addActionListener(e -> exportData());
        // Menambahkan ActionListener untuk tombol ubah
        btnUbah.addActionListener(e -> updateBarang());
        // Menambahkan ActionListener untuk tombol pilih gambar
        btnGambar.addActionListener(e -> pilihGambar());

        // Menambahkan MouseListener untuk jList3
        jList3.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Jika pengguna mengklik dua kali pada item di jList3
                if (e.getClickCount() == 2) {
                    // Mendapatkan barang yang dipilih dari jList3
                    String selectedBarang = jList3.getSelectedValue();
                    if (selectedBarang != null) {
                        // Menampilkan detail barang yang dipilih
                        showBarangDetails(selectedBarang);
                    }
                }
            }
        });
    }

    
    
      

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgKondisi = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList<>();
        tfNama = new javax.swing.JTextField();
        tfJumlah = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cbKategori = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        rbBagus = new javax.swing.JRadioButton();
        rbCukup = new javax.swing.JRadioButton();
        rbRusak = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        btnGambar = new javax.swing.JButton();
        tfGambarPath = new javax.swing.JTextField();
        lblGambar = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jPanel3 = new javax.swing.JPanel();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnImpor = new javax.swing.JButton();
        btnEkspor = new javax.swing.JButton();
        tfCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Aplikasi Inventaris Barang");
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Aplikasi Inventaris Barang", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 18))); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setViewportView(jList3);

        jLabel2.setText("Jumlah");

        jLabel1.setText("Nama");

        jLabel3.setText("Kategori");

        jLabel4.setText("Kondisi");

        bgKondisi.add(rbBagus);
        rbBagus.setText("Bagus");

        bgKondisi.add(rbCukup);
        rbCukup.setText("Cukup");

        bgKondisi.add(rbRusak);
        rbRusak.setText("Rusak");

        jLabel5.setText("Gambar");

        btnGambar.setText("Ambil");

        jLabel6.setText("Tanggal Masuk");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addGap(36, 36, 36)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tfNama)
                            .addComponent(tfJumlah)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(tfGambarPath)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnGambar))
                            .addComponent(cbKategori, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(rbBagus)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rbCukup)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(rbRusak)
                                .addGap(0, 272, Short.MAX_VALUE))
                            .addComponent(jDateChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(121, 121, 121))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(lblGambar)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jLabel1))
                            .addComponent(tfNama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(3, 3, 3)
                                .addComponent(jLabel2))
                            .addComponent(tfJumlah, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(8, 8, 8)
                                .addComponent(cbKategori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel3)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(rbBagus)
                                .addComponent(rbCukup)
                                .addComponent(rbRusak)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(btnGambar)
                            .addComponent(tfGambarPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel6)
                        .addGap(6, 6, 6))
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(lblGambar)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 459, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 31, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        btnSimpan.setText("Tambah");

        btnUbah.setText("Ubah");

        btnHapus.setText("Hapus");

        btnImpor.setText("Impor Data");

        btnEkspor.setText("Ekspor Data");

        btnCari.setText("Cari");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(tfCari, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnCari)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 147, Short.MAX_VALUE)
                .addComponent(btnImpor)
                .addGap(18, 18, 18)
                .addComponent(btnEkspor)
                .addGap(18, 18, 18)
                .addComponent(btnSimpan)
                .addGap(18, 18, 18)
                .addComponent(btnUbah)
                .addGap(18, 18, 18)
                .addComponent(btnHapus)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tfCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnUbah)
                        .addComponent(btnHapus)
                        .addComponent(btnSimpan)
                        .addComponent(btnEkspor)
                        .addComponent(btnImpor))))
        );

        jPanel1.add(jPanel3, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(InventarisBarangFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(InventarisBarangFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(InventarisBarangFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(InventarisBarangFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new InventarisBarangFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup bgKondisi;
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnEkspor;
    private javax.swing.JButton btnGambar;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnImpor;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cbKategori;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JList<String> jList3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblGambar;
    private javax.swing.JRadioButton rbBagus;
    private javax.swing.JRadioButton rbCukup;
    private javax.swing.JRadioButton rbRusak;
    private javax.swing.JTextField tfCari;
    private javax.swing.JTextField tfGambarPath;
    private javax.swing.JTextField tfJumlah;
    private javax.swing.JTextField tfNama;
    // End of variables declaration//GEN-END:variables
}
