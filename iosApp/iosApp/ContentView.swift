import UIKit
import SwiftUI
import ComposeApp

struct ComposeHostController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeHostController()
            .ignoresSafeArea(.all)
    }
}
