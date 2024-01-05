import SwiftUI

/// A view that displays movie details.
struct SampleView {

    @StateStateHolder private var viewModel: ViewModel

    @EnvironmentObject private var mainViewRouter: MainViewRouter

    init() {
        viewModel = ViewModelKt.ViewModel()
    }
}

extension SampleView: View {
    var body: some View {
        VStack {
            Image(systemName: "globe")
                .imageScale(.large)
                .foregroundColor(.accentColor)
            Text("Hello, world! from SDK")
        }
        .padding()
    }
}

struct SampleView_Previews: PreviewProvider {
    static var previews: some View {
        StackNavigationView {
            SampleView()
            .edgesIgnoringSafeArea(.vertical)
        }
        .environmentObject(mainViewRouter)
    }

    private static var mainViewRouter = MainViewRouter()
}
