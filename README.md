# 📘 TÀI LIỆU CẤU TRÚC VÀ QUY CHUẨN DỰ ÁN (MVVM + XML)

## 1. Cấu trúc thư mục (Project Structure)
Dựa trên các thư mục đã tạo, chúng ta sẽ chuẩn hóa vị trí của từng file như sau:

### 📂 data/: Nơi quản lý dữ liệu
* **model/**: Các Data Class (DTO) hứng dữ liệu từ API.
* **remote/**: Chứa RetrofitClient và các ApiService (Interface định nghĩa Endpoint).
* **repository/**: Lớp trung gian xử lý logic lấy dữ liệu (từ API hoặc Cache).

### 📂 ui/: Nơi quản lý giao diện
* **activities/**: Các màn hình chính của App (MainActivity, LoginActivity...).
* **adapters/**: Bộ chuyển đổi dữ liệu để hiển thị lên RecyclerView (Danh sách).
* **viewmodel/**: Nơi xử lý logic nghiệp vụ và giữ trạng thái dữ liệu cho UI.

### 📂 res/layout/: Tài nguyên giao diện
* Nơi chứa các file XML kéo thả giao diện.

---

## 2. Nhiệm vụ của từng bộ phận

| Bộ phận | Nhiệm vụ chính | Quy tắc "Vàng" |
| :--- | :--- | :--- |
| **XML (View)** | Hiển thị giao diện trực quan (Button, Text, List). | Không chứa bất kỳ dòng code logic nào. |
| **Activity** | Nhận tương tác người dùng và hiển thị dữ liệu từ ViewModel. | Không gọi trực tiếp API. Chỉ làm việc với ViewModel. |
| **ViewModel** | Lấy dữ liệu từ Repository, xử lý rồi đẩy vào biến LiveData. | Không chứa biến UI (như TextView, Button). |
| **Repository** | Ra lệnh cho Retrofit lấy dữ liệu từ Server. | Là nguồn dữ liệu duy nhất mà ViewModel biết tới. |
| **DTO (Model)** | Bản vẽ thiết kế dữ liệu khớp 100% với JSON của Backend. | Phải dùng @Serializable. |

---

## 3. Quy tắc đặt tên (Naming Convention)

* **Activity**: `[TênChứcNăng]Activity.kt` (Ví dụ: ProductActivity.kt).
* **XML Layout**: `activity_[tên_chức_năng].xml` (Ví dụ: activity_product.xml).
* **XML Item cho danh sách**: `item_[tên_đối_tượng].xml` (Ví dụ: item_product.xml).
* **ViewModel**: `[TênChứcNăng]ViewModel.kt` (Ví dụ: ProductViewModel.kt).
* **Repository**: `[TênChứcNăng]Repository.kt` (Ví dụ: ProductRepository.kt).
* **Adapter**: `[TênĐốiTượng]Adapter.kt` (Ví dụ: ProductAdapter.kt).

---

## 4. Luồng phát triển một chức năng mới (7 Bước)
Khi muốn làm một chức năng mới (ví dụ: Xem danh sách Sản phẩm), team sẽ đi theo đúng thứ tự này:

1. **Thiết kế DTO**: Tạo Class trong `data.model` khớp với JSON API.
2. **Định nghĩa API**: Thêm hàm gọi API trong `ProductApiService`.
3. **Viết Repository**: Tạo hàm trong `ProductRepository` để gọi `ApiService`.
4. **Viết ViewModel**: Gọi Repository, lưu dữ liệu trả về vào một biến `LiveData` (để Activity quan sát).
5. **Thiết kế XML**: Kéo thả giao diện trong `res/layout` (gồm file màn hình chính và file từng dòng item).
6. **Viết Adapter**: Code bộ dịch để đưa danh sách dữ liệu vào `RecyclerView`.
7. **Kết nối Activity**: Gọi ViewModel và nạp dữ liệu vào giao diện.

---

## 🔄 Ví dụ về Luồng dữ liệu (Data Flow)

> **Luồng chạy**:
> Người dùng mở App → **Activity** yêu cầu **ViewModel** lấy hàng → **ViewModel** hỏi **Repository** → **Repository** lấy từ **API** → Dữ liệu quay ngược lại **ViewModel** → **Activity** thấy dữ liệu thay đổi → Ra lệnh cho **Adapter** hiển thị lên màn hình.
