import { ref, computed } from 'vue'

const STORAGE_KEY = 'adha_store_locale'
const SUPPORTED = ['vi', 'en']

export function getSavedStoreLocale() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved && SUPPORTED.includes(saved)) return saved
  } catch {
    // ignore storage errors
  }
  return 'vi'
}

const currentLocale = ref(getSavedStoreLocale())

export function setStoreLocale(lang) {
  const normalized = SUPPORTED.includes(lang) ? lang : 'vi'
  currentLocale.value = normalized
  try {
    localStorage.setItem(STORAGE_KEY, normalized)
    if (typeof document !== 'undefined') {
      document.documentElement.lang = normalized
    }
  } catch {
    // ignore
  }
}

const MESSAGES = {
  vi: {
    promo: {
      m1: 'TRẢ HÀNG DỄ DÀNG',
      m2: 'GIAO HÀNG MIỄN PHÍ CHO THÀNH VIÊN',
      m3: '100% SNEAKER CHÍNH HÃNG — KIỂM ĐỊNH LEGIT CHECK',
      sheet1Title: 'TRẢ HÀNG & ĐỔI SIZE DỄ DÀNG',
      sheet1Desc: 'Hỗ trợ đổi size miễn phí hoặc hoàn trả linh hoạt trong 7 ngày kể từ khi nhận hàng nếu sản phẩm còn nguyên tem mác.',
      sheet1Cta: 'XEM BIỂU ĐỒ KÍCH CỠ',
      sheet2Title: 'MIỄN PHÍ GIAO HÀNG TOÀN QUỐC',
      sheet2Desc: 'Freeship cho mọi đơn hàng từ 1.000.000₫. Đóng gói 2 lớp hộp (Double-Box) bảo vệ hộp giày nguyên bản.',
      sheet2Cta: 'MUA SẮM NGAY',
      sheet3Title: '100% SNEAKER CHÍNH HÃNG',
      sheet3Desc: 'Kiểm định Legit Check nghiêm ngặt trước khi xuất kho. Hoàn tiền 200% nếu phát hiện hàng giả mạo.',
      sheet3Cta: 'TRÌNH THEO DÕI ĐƠN HÀNG'
    },
    utility: {
      storeFinder: 'tìm cửa hàng',
      help: 'trợ giúp',
      orderTracker: 'trình theo dõi đơn hàng',
      becomeMember: 'đăng ký hội viên',
      admin: 'quản trị'
    },
    lang: {
      title: 'Chọn ngôn ngữ của bạn',
      save: 'Lưu'
    },
    nav: {
      sale: 'BLACK FRIDAY SALE',
      men: 'NAM',
      women: 'NỮ',
      newArrivals: 'HÀNG MỚI',
      sports: 'THỂ THAO',
      brands: 'CÁC NHÃN HIỆU',
      outlet: 'OUTLET',
      searchPlaceholder: 'tìm kiếm',
      searchBtn: 'TÌM'
    },
    mega: {
      featured: 'NỔI BẬT',
      saleLink: 'Black Friday Sale — Giảm đến 50%',
      newLink: 'Hàng Mới Về',
      topSellers: 'Top Sellers (Bán chạy nhất)',
      allShoes: 'Tất Cả Giày Sneaker',
      sizeChart: 'Biểu Đồ Kích Cỡ',
      brands: 'CÁC NHÃN HIỆU',
      sports: 'THỂ THAO',
      orderTracker: 'Trình Theo Dõi Đơn Hàng',
      myOrders: 'Đơn Hàng Của Tôi',
      exploreNow: 'Khám phá ngay'
    },
    account: {
      memberSub: 'Thành viên ADHA Club',
      myOrders: 'Đơn hàng của tôi',
      activeOrdersSuffix: 'đang xử lý',
      personalVouchers: 'Voucher cá nhân của bạn',
      wishlist: 'Sản phẩm yêu thích',
      orderTracker: 'Trình theo dõi đơn hàng',
      profile: 'Thông tin tài khoản',
      logout: 'Đăng xuất',
      loggedOut: 'Đã đăng xuất tài khoản',
      mobileLoginTitle: 'ĐĂNG NHẬP / ĐĂNG KÝ HỘI VIÊN',
      mobileLoginSub: 'Nhận ưu đãi hội viên 10% & quản lý đơn',
      mobileSupport: 'HỖ TRỢ'
    },
    club: {
      headline: 'TRỞ THÀNH HỘI VIÊN & HƯỞNG ƯU ĐÃI 10%',
      registerCta: 'ĐĂNG KÝ MIỄN PHÍ',
      shopCta: 'KHÁM PHÁ ƯU ĐÃI'
    },
    footer: {
      products: 'SẢN PHẨM',
      shoes: 'Giày',
      newArrivals: 'Hàng Mới Về',
      releaseDates: 'Release Dates',
      topSellers: 'Top Sellers',
      memberExclusives: 'Member exclusives',
      outlet: 'Outlet',
      sports: 'THỂ THAO',
      collections: 'BỘ SƯU TẬP',
      company: 'THÔNG TIN VỀ CÔNG TY',
      about: 'Giới Thiệu Về Chúng Tôi',
      careers: 'Cơ Hội Nghề Nghiệp',
      news: 'Tin tức',
      stories: 'ADHA stories',
      adminPortal: 'Kênh Quản Trị',
      support: 'HỖ TRỢ',
      help: 'Trợ Giúp',
      tracker: 'Công cụ theo dõi đơn hàng',
      sizeChart: 'Biểu Đồ Kích Cỡ',
      payment: 'Thanh toán',
      deliveryReturns: 'Giao hàng & Trả Hàng',
      wishlist: 'Danh sách yêu thích',
      followUs: 'THEO DÕI CHÚNG TÔI',
      cookie: 'Cài Đặt Cookie',
      privacy: 'Chính sách Bảo mật',
      terms: 'Điều Khoản và Điều Kiện',
      imprint: 'XUẤT BẢN BỞI',
      copyright: '© 2026 Công ty TNHH ADHA Sneaker Việt Nam',
      sizeDialogTitle: 'BIỂU ĐỒ KÍCH CỠ GIÀY CHUẨN (SIZE CHART)',
      sizeDialogTip: 'Đo chiều dài bàn chân từ gót chân đến ngón dài nhất để lựa chọn size chính xác nhất:',
      footLength: 'Chiều dài bàn chân'
    },
    home: {
      bfTitle: 'BLACK FRIDAY | BÙNG NỔ ƯU ĐÃI',
      bfDesc: 'Giảm đến 50% cho các mẫu Sneaker BLACK FRIDAY, đồng thời nhận thêm 10% ưu đãi voucher khi thanh toán. Áp dụng toàn hệ thống ADHA Sneaker.',
      bfCta: 'MUA TẤT CẢ ƯU ĐÃI',
      menPill: 'ĐỒ NAM',
      womenPill: 'ĐỒ NỮ',
      colPill: 'BỘ SƯU TẬP',
      deal1Sub: 'SIZE CUỐI · GIẢM NGAY',
      deal1Title: 'SIZE CUỐI - GIẢM NGAY 50%',
      deal2Sub: 'STREETWEAR ARCHIVE',
      deal2Big: 'THỜI TRANG CHẤT, GU XỊN',
      deal2Title: 'THỜI TRANG CHẤT, GU XỊN',
      buyNow: 'Mua ngay',
      hero1Title: 'ADHA COLLABORATIONS ARCHIVE',
      hero1Sub: 'Quyền truy cập độc quyền vào kho Sneaker chính hãng đã mở. Tuyển chọn phiên bản giới hạn từ Adidas, Nike, Jordan, New Balance với ưu đãi voucher đến 50%.',
      hero1Cta: 'KHÁM PHÁ',
      tabSale: 'Archive sale',
      tabNew: 'Hàng Mới Về',
      seeAll: 'Xem tất cả',
      origPrice: 'Giá gốc',
      hero2Title: 'GIÀY ORIGINALS & RUNNING',
      hero2Sub: 'Đôi giày hằng ngày với phom dáng cổ điển, êm ái và linh hoạt cho mọi phong cách.',
      shopNowBtn: 'MUA NGAY',
      exploreMoreBtn: 'KHÁM PHÁ THÊM',
      iconsHeading: 'STEP INTO YOUR NEXT ICON',
      whatsHotHeading: "WHAT'S HOT",
      stillInterestedHeading: 'BẠN VẪN QUAN TÂM?',
      seoCol1: 'GIÀY',
      seoCol2: 'THƯƠNG HIỆU',
      seoCol3: 'DÒNG SẢN PHẨM',
      seoCol4: 'DỊCH VỤ',
      seoRun: 'giày chạy bộ',
      seoLife: 'giày lifestyle',
      seoBsk: 'giày bóng rổ',
      seoNew: 'giày hàng mới về',
      seoTracker: 'trình theo dõi đơn hàng',
      seoOrders: 'đơn hàng của tôi',
      seoCart: 'giỏ hàng & voucher',
      seoOutlet: 'ưu đãi outlet',
      storyHeading: 'CỬA HÀNG GIÀY SNEAKER ADHA – HIỆU NĂNG, PHONG CÁCH & ĐỔI MỚI',
      storyP1: 'Thể thao giúp chúng ta khỏe mạnh. Tinh thần tỉnh táo. Kết nối mọi người. Qua từng đôi giày sneaker chính hãng, chúng tôi mang đến sự tự tin cho từng bước chân – bằng thiết kế biểu tượng, công nghệ đệm tiên tiến và tinh thần đường phố không giới hạn.',
      storyP2: 'Khám phá bộ sưu tập giày thể thao hiệu năng cao và thời trang đường phố từ Adidas, Nike, Air Jordan, New Balance, Asics và Converse. Dù bạn là người yêu thích chạy bộ, bóng rổ hay đơn giản là tìm kiếm một đôi sneaker phối đồ mỗi ngày, ADHA Sneaker luôn là điểm đến chuẩn mực.',
      storyP3: 'Mỗi sản phẩm tại ADHA Sneaker đều được kiểm định Legit Check 100%, đóng gói Double-Box bảo vệ hộp nguyên bản và hỗ trợ đổi size trong 7 ngày. Miễn phí vận chuyển toàn quốc cho đơn hàng từ 1.000.000₫ cùng hệ thống thanh toán VietQR tự động và COD tiện lợi.'
    },
    catalog: {
      back: '← QUAY LẠI',
      home: 'Trang chủ',
      shoes: 'Giày Sneaker',
      desc: 'Khám phá bộ sưu tập giày Sneaker chính hãng mới nhất tại ADHA Sneaker — đầy đủ phối màu, kích cỡ và kiểm định chuẩn Legit Check.',
      all: 'Tất cả',
      sortNewest: 'SẮP XẾP: MỚI NHẤT',
      sortAsc: 'GIÁ: THẤP ĐẾN CAO',
      sortDesc: 'GIÁ: CAO ĐẾN THẤP',
      filterBtn: 'BỘ LỌC',
      searchLabel: 'TÌM THEO TÊN / MÃ SKU',
      searchPlaceholder: 'VD: Air Force 1, Samba...',
      brandsLabel: 'THƯƠNG HIỆU',
      categoriesLabel: 'DÒNG SẢN PHẨM',
      clear: 'Xóa',
      clearAll: 'Xóa tất cả',
      allBrands: 'Tất cả thương hiệu',
      resetAll: 'XÓA TẤT CẢ BỘ LỌC',
      emptyTitle: 'KHÔNG TÌM THẤY SẢN PHẨM PHÙ HỢP',
      emptyDesc: 'Thử từ khóa khác hoặc đặt lại bộ lọc để xem toàn bộ sản phẩm đang sẵn kho.',
      prevPage: '← TRANG TRƯỚC',
      nextPage: 'TRANG SAU →',
      titleOutlet: 'OUTLET — GIÁ TỐT NHẤT',
      titleNewest: 'HÀNG MỚI VỀ',
      titleAll: 'TẤT CẢ GIÀY SNEAKER',
      titleSearch: 'KẾT QUẢ CHO',
      titleBrandPrefix: 'GIÀY'
    },
    pdp: {
      back: 'Trở lại',
      home: 'Trang Chủ',
      showMore: 'Hiện thêm',
      showLess: 'Thu gọn',
      reviews: 'Đánh giá',
      description: 'Mô tả',
      details: 'Thông tin chi tiết',
      comfort: 'Độ êm',
      quality: 'Chất lượng',
      size: 'Kích cỡ',
      width: 'Độ rộng',
      uncomfortable: 'Chưa êm',
      comfortable: 'Rất êm ái',
      poor: 'Trung bình',
      perfect: 'Hoàn hảo',
      tooSmall: 'Hơi chật',
      trueToSize: 'Chuẩn size',
      tooLarge: 'Hơi rộng',
      tooNarrow: 'Hơi ôm',
      fitJustRight: 'Vừa vặn',
      tooWide: 'Thoải mái',
      onFeetTitle: 'Ảnh On-Feet thực tế',
      filterByStar: 'Lọc theo xếp loại sao',
      filterAll: 'Tất cả',
      filterHasPhoto: 'Có ảnh On-feet',
      verifiedBuyer: 'Đã xác minh mua hàng',
      helpful: 'Hữu ích',
      emptyReviews: 'Chưa có đánh giá nào phù hợp với bộ lọc.',
      writeReviewLink: 'Vào Đơn hàng của tôi để viết đánh giá →',
      specFit: 'Dáng regular fit tiêu chuẩn',
      specLace: 'Có dây giày chắc chắn',
      specMaterial: 'Chất liệu',
      specBrand: 'Thương hiệu',
      specCategory: 'Phân loại',
      specColor: 'Màu sản phẩm',
      specSku: 'Mã sản phẩm',
      specStock: 'Tình trạng kho',
      inStockText: 'đôi sẵn hàng',
      outOfStock: 'Tạm hết hàng',
      completeLook: 'HOÀN THIỆN PHONG CÁCH',
      legitTag: 'CHÍNH HÃNG 100% · KIỂM ĐỊNH LEGIT CHECK',
      colors: 'Màu sắc',
      sizes: 'Kích cỡ',
      smartSizeFinder: 'Gợi ý size',
      sizeGuide: 'Hướng dẫn chọn kích cỡ',
      selectSizeErr: 'Vui lòng chọn kích cỡ của bạn trước khi thêm vào giỏ hàng.',
      fitAdviceStrong: 'Đúng kích cỡ.',
      fitAdviceBody: 'Chúng tôi khuyên bạn nên đặt theo kích cỡ thông thường.',
      quantity: 'Số lượng',
      addToBag: 'THÊM VÀO GIỎ HÀNG',
      buyNow: 'Mua ngay',
      memberEarn: 'Thành viên có thể nhận',
      pointsLabel: 'Điểm ADHA Club',
      usp1: 'BLACK FRIDAY | BÙNG NỔ ƯU ĐÃI',
      usp2: 'ĐỔI SIZE DỄ DÀNG TRONG 7 NGÀY',
      usp3: 'MIỄN PHÍ GIAO HÀNG ĐƠN TỪ 1.000.000₫',
      shopMorePrefix: 'Xem thêm Giày'
    }
  },
  en: {
    promo: {
      m1: 'EASY RETURNS & EXCHANGES',
      m2: 'FREE SHIPPING FOR ADHA CLUB MEMBERS',
      m3: '100% AUTHENTIC SNEAKERS — LEGIT CHECK VERIFIED',
      sheet1Title: 'EASY 7-DAY SIZE EXCHANGES',
      sheet1Desc: 'Free size exchange or hassle-free returns within 7 days of delivery with original tags attached.',
      sheet1Cta: 'VIEW SIZE GUIDE',
      sheet2Title: 'FREE NATIONWIDE SHIPPING',
      sheet2Desc: 'Complimentary Double-Box shipping on all orders from 1,000,000₫ to protect your sneaker box.',
      sheet2Cta: 'SHOP NOW',
      sheet3Title: '100% VERIFIED AUTHENTIC',
      sheet3Desc: 'Every pair passes strict Legit Check verification before dispatch. 200% money-back authenticity guarantee.',
      sheet3Cta: 'TRACK YOUR ORDER'
    },
    utility: {
      storeFinder: 'store finder',
      help: 'help',
      orderTracker: 'order tracker',
      becomeMember: 'become a member',
      admin: 'admin'
    },
    lang: {
      title: 'Choose your language',
      save: 'Save'
    },
    nav: {
      sale: 'BLACK FRIDAY SALE',
      men: 'MEN',
      women: 'WOMEN',
      newArrivals: 'NEW ARRIVALS',
      sports: 'SPORTS',
      brands: 'BRANDS',
      outlet: 'OUTLET',
      searchPlaceholder: 'Search',
      searchBtn: 'GO'
    },
    mega: {
      featured: 'FEATURED',
      saleLink: 'Black Friday Sale — Up to 50% Off',
      newLink: 'New Arrivals',
      topSellers: 'Top Sellers',
      allShoes: 'All Sneakers',
      sizeChart: 'Size Chart',
      brands: 'BRANDS',
      sports: 'SPORTS',
      orderTracker: 'Order Tracker',
      myOrders: 'My Orders',
      exploreNow: 'Explore now'
    },
    account: {
      memberSub: 'ADHA Club Member',
      myOrders: 'My Orders',
      activeOrdersSuffix: 'in progress',
      personalVouchers: 'Your personal vouchers',
      wishlist: 'Wishlist',
      orderTracker: 'Order Tracker',
      profile: 'Account Profile',
      logout: 'Sign out',
      loggedOut: 'Signed out successfully',
      mobileLoginTitle: 'SIGN IN / JOIN ADHA CLUB',
      mobileLoginSub: 'Get 10% member voucher & track orders',
      mobileSupport: 'SUPPORT'
    },
    club: {
      headline: 'JOIN ADHA CLUB & GET 10% OFF',
      registerCta: 'SIGN UP FOR FREE',
      shopCta: 'EXPLORE OFFERS'
    },
    footer: {
      products: 'PRODUCTS',
      shoes: 'Shoes',
      newArrivals: 'New Arrivals',
      releaseDates: 'Release Dates',
      topSellers: 'Top Sellers',
      memberExclusives: 'Member exclusives',
      outlet: 'Outlet',
      sports: 'SPORTS',
      collections: 'COLLECTIONS',
      company: 'COMPANY INFO',
      about: 'About Us',
      careers: 'Careers',
      news: 'Press & News',
      stories: 'ADHA stories',
      adminPortal: 'Admin Portal',
      support: 'SUPPORT',
      help: 'Help',
      tracker: 'Order Tracker',
      sizeChart: 'Size Charts',
      payment: 'Payment Methods',
      deliveryReturns: 'Delivery & Returns',
      wishlist: 'Wishlist',
      followUs: 'FOLLOW US',
      cookie: 'Cookie Settings',
      privacy: 'Privacy Policy',
      terms: 'Terms and Conditions',
      imprint: 'IMPRINT',
      copyright: '© 2026 ADHA Sneaker Vietnam Co., Ltd.',
      sizeDialogTitle: 'SHOE SIZE CONVERSION CHART',
      sizeDialogTip: 'Measure your foot length from heel to longest toe to find your ideal EU size:',
      footLength: 'Foot Length'
    },
    home: {
      bfTitle: 'BLACK FRIDAY | MEGA DEALS',
      bfDesc: 'Save up to 50% on curated BLACK FRIDAY sneakers, plus unlock an extra 10% member voucher at checkout across ADHA Sneaker.',
      bfCta: 'SHOP ALL DEALS',
      menPill: 'MEN',
      womenPill: 'WOMEN',
      colPill: 'ARCHIVE',
      deal1Sub: 'LAST SIZES · SAVE UP TO',
      deal1Title: 'LAST SIZES - SAVE 50% NOW',
      deal2Sub: 'STREETWEAR ARCHIVE',
      deal2Big: 'ICONIC STYLE, BOLD LOOK',
      deal2Title: 'ICONIC STYLE, BOLD LOOK',
      buyNow: 'Shop now',
      hero1Title: 'ADHA COLLABORATIONS ARCHIVE',
      hero1Sub: 'Your exclusive access to our verified sneaker archive is now open. Discover limited releases from Adidas, Nike, Jordan & New Balance with up to 50% off.',
      hero1Cta: 'EXPLORE',
      tabSale: 'Archive sale',
      tabNew: 'New Arrivals',
      seeAll: 'View all',
      origPrice: 'Original',
      hero2Title: 'ORIGINALS & RUNNING ICONS',
      hero2Sub: 'Everyday icons crafted with classic silhouettes, responsive cushioning and effortless versatility.',
      shopNowBtn: 'SHOP NOW',
      exploreMoreBtn: 'EXPLORE MORE',
      iconsHeading: 'STEP INTO YOUR NEXT ICON',
      whatsHotHeading: "WHAT'S HOT",
      stillInterestedHeading: 'STILL INTERESTED?',
      seoCol1: 'SHOES',
      seoCol2: 'BRANDS',
      seoCol3: 'CATEGORIES',
      seoCol4: 'SERVICES',
      seoRun: 'running shoes',
      seoLife: 'lifestyle sneakers',
      seoBsk: 'basketball shoes',
      seoNew: 'new arrival sneakers',
      seoTracker: 'order tracker',
      seoOrders: 'my orders',
      seoCart: 'shopping bag & vouchers',
      seoOutlet: 'outlet deals',
      storyHeading: 'ADHA SNEAKER STORE – PERFORMANCE, CULTURE & INNOVATION',
      storyP1: 'Sport keeps us moving. Street culture connects us. Through every authentic pair of sneakers, we empower your stride with iconic silhouettes, advanced cushioning, and timeless craft.',
      storyP2: 'Explore performance running, basketball, and everyday streetwear classics from Adidas, Nike, Air Jordan, New Balance, Asics, and Converse — curated under one roof for true sneaker enthusiasts.',
      storyP3: 'Every pair dispatched by ADHA Sneaker is 100% Legit Check verified, shipped Double-Boxed nationwide, and backed by 7-day size exchanges plus seamless VietQR and COD checkout.'
    },
    catalog: {
      back: '← BACK',
      home: 'Home',
      shoes: 'Sneakers',
      desc: 'Explore the latest verified authentic sneakers at ADHA Sneaker — full size runs, iconic colorways, and Double-Box delivery.',
      all: 'All',
      sortNewest: 'SORT: NEWEST',
      sortAsc: 'PRICE: LOW TO HIGH',
      sortDesc: 'PRICE: HIGH TO LOW',
      filterBtn: 'FILTER',
      searchLabel: 'SEARCH BY NAME / SKU',
      searchPlaceholder: 'e.g. Air Force 1, Samba...',
      brandsLabel: 'BRANDS',
      categoriesLabel: 'CATEGORIES',
      clear: 'Clear',
      clearAll: 'Clear all',
      allBrands: 'All brands',
      resetAll: 'RESET ALL FILTERS',
      emptyTitle: 'NO MATCHING PRODUCTS FOUND',
      emptyDesc: 'Try another keyword or reset your filters to browse all in-stock sneakers.',
      prevPage: '← PREVIOUS',
      nextPage: 'NEXT →',
      titleOutlet: 'OUTLET — BEST DEALS',
      titleNewest: 'NEW ARRIVALS',
      titleAll: 'ALL SNEAKERS',
      titleSearch: 'RESULTS FOR',
      titleBrandPrefix: ''
    },
    pdp: {
      back: 'Back',
      home: 'Home',
      showMore: 'Show more',
      showLess: 'Show less',
      reviews: 'Reviews',
      description: 'Description',
      details: 'Details',
      comfort: 'Comfort',
      quality: 'Quality',
      size: 'Size',
      width: 'Width',
      uncomfortable: 'Uncomfortable',
      comfortable: 'Comfortable',
      poor: 'Poor',
      perfect: 'Perfect',
      tooSmall: 'Too small',
      trueToSize: 'Perfect',
      tooLarge: 'Too large',
      tooNarrow: 'Too narrow',
      fitJustRight: 'Perfect',
      tooWide: 'Too wide',
      onFeetTitle: 'Customer On-Feet Photos',
      filterByStar: 'Filter by rating',
      filterAll: 'All',
      filterHasPhoto: 'With photos',
      verifiedBuyer: 'Verified purchaser',
      helpful: 'Helpful',
      emptyReviews: 'No reviews match this filter yet.',
      writeReviewLink: 'Go to My Orders to write a review →',
      specFit: 'Regular fit',
      specLace: 'Lace closure',
      specMaterial: 'Material',
      specBrand: 'Brand',
      specCategory: 'Category',
      specColor: 'Colorway',
      specSku: 'Product code',
      specStock: 'Availability',
      inStockText: 'pairs in stock',
      outOfStock: 'Out of stock',
      completeLook: 'COMPLETE THE LOOK',
      legitTag: '100% AUTHENTIC · LEGIT CHECK VERIFIED',
      colors: 'Colors',
      sizes: 'Sizes',
      smartSizeFinder: 'Size Finder',
      sizeGuide: 'Size guide',
      selectSizeErr: 'Please select your size before adding to bag.',
      fitAdviceStrong: 'True to size.',
      fitAdviceBody: 'We recommend ordering your usual size.',
      quantity: 'Quantity',
      addToBag: 'ADD TO BAG',
      buyNow: 'Buy now',
      memberEarn: 'Members can earn',
      pointsLabel: 'ADHA Club Points',
      usp1: 'BLACK FRIDAY | MEGA DEALS',
      usp2: 'EASY 7-DAY SIZE EXCHANGES',
      usp3: 'FREE SHIPPING ON ORDERS FROM 1,000,000₫',
      shopMorePrefix: 'Shop More'
    }
  }
}

export function useStoreI18n() {
  const locale = computed(() => currentLocale.value)
  const isEn = computed(() => currentLocale.value === 'en')

  const t = (pathKey) => {
    const parts = String(pathKey).split('.')
    let cur = MESSAGES[currentLocale.value] || MESSAGES.vi
    for (const p of parts) {
      cur = cur?.[p]
      if (cur === undefined) break
    }
    if (cur !== undefined) return cur
    let fallback = MESSAGES.vi
    for (const p of parts) {
      fallback = fallback?.[p]
      if (fallback === undefined) return pathKey
    }
    return fallback
  }

  const lp = (subPath = '') => {
    const clean = String(subPath || '').replace(/^\/+/, '')
    return clean ? `/${currentLocale.value}/${clean}` : `/${currentLocale.value}`
  }

  const switchLocale = (nextLang, router, route) => {
    const target = SUPPORTED.includes(nextLang) ? nextLang : 'vi'
    setStoreLocale(target)
    if (!router || !route) return
    const currentPath = route.path || '/'
    let nextPath = `/${target}`
    if (/^\/(vi|en)(\/|$)/.test(currentPath)) {
      nextPath = currentPath.replace(/^\/(vi|en)(?=\/|$)/, `/${target}`)
    } else if (!currentPath.startsWith('/admin') && !currentPath.startsWith('/pos')) {
      nextPath = currentPath === '/' ? `/${target}` : `/${target}${currentPath}`
    }
    router.push({ path: nextPath, query: route.query, hash: route.hash })
  }

  return {
    locale,
    isEn,
    t,
    lp,
    setStoreLocale,
    switchLocale
  }
}
