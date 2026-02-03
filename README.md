# [cite_start]📘 TÀI LIỆU CẤU TRÚC VÀ QUY CHUẨN DỰ ÁN (MVVM + XML) [cite: 1]

## [cite_start]1. Cấu trúc thư mục (Project Structure) [cite: 2]
[cite_start]Dựa trên các thư mục đã tạo, chúng ta sẽ chuẩn hóa vị trí của từng file như sau[cite: 3]:

### [cite_start]📂 `data/`: Nơi quản lý dữ liệu [cite: 4]
* [cite_start]**`model/`**: Các Data Class (DTO) hứng dữ liệu từ API[cite: 5].
* [cite_start]**`remote/`**: Chứa `RetrofitClient` và các `ApiService` (Interface định nghĩa Endpoint)[cite: 6].
* [cite_start]**`repository/`**: Lớp trung gian xử lý logic lấy dữ liệu (từ API hoặc Cache)[cite: 7].

### [cite_start]📂 `ui/`: Nơi quản lý giao diện [cite: 8]
* [cite_start]**`activities/`**: Các màn hình chính của App (MainActivity, LoginActivity...)[cite: 9].
* [cite_start]**`adapters/`**: Bộ chuyển đổi dữ liệu để hiển thị lên `RecyclerView` (Danh sách)[cite: 10].
* [cite_start]**`viewmodel/`**: Nơi xử lý logic nghiệp vụ và giữ trạng thái dữ liệu cho UI[cite: 11].

### [cite_start]📂 `res/layout/`: Tài nguyên giao diện [cite: 12]
* [cite_start]Nơi chứa các file XML kéo thả giao diện[cite: 12].

---

## [cite_start]2. Nhiệm vụ của từng bộ phận [cite: 13]

| Bộ phận | Nhiệm vụ chính | Quy tắc "Vàng" |
| :--- | :--- | :--- |
| **XML (View)** | [cite_start]Hiển thị giao diện trực quan (Button, Text, List)[cite: 14]. | [cite_start]Không chứa bất kỳ dòng code logic nào[cite: 14]. |
| **Activity** | [cite_start]Nhận tương tác người dùng và hiển thị dữ liệu từ ViewModel[cite: 14]. | Không gọi trực tiếp API. [cite_start]Chỉ làm việc với ViewModel[cite: 14]. |
| **ViewModel** | [cite_start]Lấy dữ liệu từ Repository, xử lý rồi đẩy vào biến LiveData[cite: 14]. | [cite_start]Không chứa biến UI (như TextView, Button)[cite: 14]. |
| **Repository** | [cite_start]Ra lệnh cho Retrofit lấy dữ liệu từ Server[cite: 14]. | [cite_start]Là nguồn dữ liệu duy nhất mà ViewModel biết tới[cite: 14]. |
| **DTO (Model)** | [cite_start]Bản vẽ thiết kế dữ liệu khớp 100% với JSON của Backend[cite: 14]. | [cite_start]Phải dùng `@Serializable`[cite: 14]. |

---

## [cite_start]3. Quy tắc đặt tên (Naming Convention) [cite: 15]

* [cite_start]**Activity**: `[TênChứcNăng]Activity.kt` (Ví dụ: `ProductActivity.kt`)[cite: 16].
* [cite_start]**XML Layout**: `activity_[tên_chức_năng].xml` (Ví dụ: `activity_product.xml`)[cite: 17].
* [cite_start]**XML Item cho danh sách**: `item_[tên_đối_tượng].xml` (Ví dụ: `item_product.xml`)[cite: 18].
* [cite_start]**ViewModel**: `[TênChứcNăng]ViewModel.kt` (Ví dụ: `ProductViewModel.kt`)[cite: 19].
* [cite_start]**Repository**: `[TênChứcNăng]Repository.kt` (Ví dụ: `ProductRepository.kt`)[cite: 20].
* [cite_start]**Adapter**: `[TênĐốiTượng]Adapter.kt` (Ví dụ: `ProductAdapter.kt`)[cite: 21].

---

## [cite_start]4. Luồng phát triển một chức năng mới (7 Bước) [cite: 22]
[cite_start]Khi muốn làm một chức năng mới (ví dụ: Xem danh sách Sản phẩm), team sẽ đi theo đúng thứ tự này[cite: 23]:

1. [cite_start]**Thiết kế DTO**: Tạo Class trong `data.model` khớp với JSON API[cite: 24].
2. [cite_start]**Định nghĩa API**: Thêm hàm gọi API trong `ProductApiService`[cite: 25].
3. [cite_start]**Viết Repository**: Tạo hàm trong `ProductRepository` để gọi `ApiService`[cite: 26].
4. [cite_start]**Viết ViewModel**: Gọi Repository, lưu dữ liệu trả về vào một biến `LiveData` (để Activity quan sát)[cite: 27].
5. [cite_start]**Thiết kế XML**: Kéo thả giao diện trong `res/layout` (gồm file màn hình chính và file từng dòng item)[cite: 28].
6. [cite_start]**Viết Adapter**: Code bộ dịch để đưa danh sách dữ liệu vào `RecyclerView`[cite: 29].
7. [cite_start]**Kết nối Activity**: Gọi ViewModel và nạp dữ liệu vào giao diện[cite: 30].

---

## [cite_start]🔄 Ví dụ về Luồng dữ liệu (Data Flow) [cite: 31]



> **Luồng chạy**:
> [cite_start]Người dùng mở App → **Activity** yêu cầu **ViewModel** lấy hàng → **ViewModel** hỏi **Repository** → **Repository** lấy từ **API** → Dữ liệu quay ngược lại **ViewModel** → **Activity** thấy dữ liệu thay đổi → Ra lệnh cho **Adapter** hiển thị lên màn hình[cite: 32].
