import UIKit

class ItemDetailViewController: UIViewController {

    var itemId: Int = -1

    // Labels
    private let titleLabel    = UILabel()
    private let priceLabel    = UILabel()
    private let categoryLabel = UILabel()
    private let descLabel     = UILabel()
    private let sellerLabel   = UILabel()
    private let dateLabel     = UILabel()
    private let soldBadge     = UILabel()
    private let scrollView    = UIScrollView()
    private let stackView     = UIStackView()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Item Detail"
        view.backgroundColor = .systemBackground
        setupUI()
        fetchItem()
    }

    private func setupUI() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        stackView.axis = .vertical
        stackView.spacing = 8
        stackView.layoutMargins = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        stackView.isLayoutMarginsRelativeArrangement = true
        stackView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(scrollView)
        scrollView.addSubview(stackView)

        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            stackView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            stackView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            stackView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            stackView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
        ])

        // Style
        titleLabel.font = .boldSystemFont(ofSize: 22)
        titleLabel.numberOfLines = 0

        priceLabel.font = .boldSystemFont(ofSize: 20)
        priceLabel.textColor = .systemGreen

        categoryLabel.textColor = .secondaryLabel
        categoryLabel.font = .systemFont(ofSize: 14)

        descLabel.numberOfLines = 0
        descLabel.font = .systemFont(ofSize: 16)

        sellerLabel.textColor = .secondaryLabel
        dateLabel.textColor = .secondaryLabel

        soldBadge.text = "SOLD"
        soldBadge.textColor = .white
        soldBadge.backgroundColor = .systemRed
        soldBadge.font = .boldSystemFont(ofSize: 16)
        soldBadge.textAlignment = .center
        soldBadge.layer.cornerRadius = 4
        soldBadge.clipsToBounds = true
        soldBadge.isHidden = true

        [soldBadge, titleLabel, priceLabel, categoryLabel,
         descLabel, sellerLabel, dateLabel].forEach {
            stackView.addArrangedSubview($0)
        }
    }

    private func fetchItem() {
        APIClient.getItem(id: itemId) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let item):
                    self?.populate(item)
                case .failure(let error):
                    self?.showError(error.localizedDescription)
                }
            }
        }
    }

    private func populate(_ item: Item) {
        titleLabel.text    = item.title
        priceLabel.text    = item.formattedPrice
        categoryLabel.text = "Category: \(item.category)"
        descLabel.text     = item.description ?? "(no description)"
        sellerLabel.text   = "Seller: \(item.sellerName)"
        dateLabel.text     = "Posted: \(item.createdAt)"
        soldBadge.isHidden = !item.isSold
    }

    private func showError(_ message: String) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}